# Terraform: EKS cluster on the AWS Academy Learner Lab

Terraform builds the **infrastructure**: an empty Kubernetes cluster on AWS.
Jenkins then builds the **application** and deploys it onto that cluster.

| | Terraform (this folder) | Jenkins ([`../Jenkinsfile`](../Jenkinsfile)) |
| --- | --- | --- |
| Creates | EKS cluster + 2 worker nodes | Tested JAR, Docker images, running pods |
| Run it | Once per lab session (`apply`), once at the end (`destroy`) | At every code change |
| Needs | Learner Lab credentials on your PC | Learner Lab credentials in Jenkins |

You do **not** run Terraform at every build. Create the cluster once, deploy
with Jenkins as many times as you want, then destroy the cluster.

## What gets created

| Resource | Purpose |
| --- | --- |
| `aws_eks_cluster.achat` | Kubernetes control plane named `achat-eks` |
| `aws_eks_node_group.achat` | 2 × `t3.medium` worker nodes that run the pods |
| `aws_ec2_tag.elb_subnet` | Subnet tags so Kubernetes can create the public load balancer for the frontend |

Everything goes into the default VPC of the lab account.

## Learner Lab rules this code follows

- **Regions:** only `us-east-1` and `us-west-2` (Terraform refuses others).
- **No IAM creation:** the cluster and nodes reuse the existing `LabRole`.
- **AZs:** EKS rejects `us-east-1e`, so only AZs `a` to `d` are used.
- **Credentials expire:** every lab session (about 4 hours) gives new keys.
  Old keys fail with `ExpiredToken` or `explicit deny ... voc-cancel-cred`.
- **Budget ($50):** the cluster costs about **$0.20/hour even when idle**
  (control plane + nodes + load balancer). Always destroy it after a demo.

## One-time setup

Install the tools (Windows PowerShell):

```powershell
winget install Hashicorp.Terraform Amazon.AWSCLI Kubernetes.kubectl
```

Close and reopen the terminal, then check:

```powershell
terraform -version
aws --version
kubectl version --client
```

## Step 1 - Start the lab and copy the credentials

Do this **at the start of every lab session**.

1. Open the Learner Lab page and click **Start Lab**. Wait until the dot next
   to **AWS** is **green**.
2. Click **AWS Details**, then **Show** next to **AWS CLI**.
3. Copy the whole block (4 lines):

   ```
   [default]
   aws_access_key_id=ASIA...
   aws_secret_access_key=...
   aws_session_token=IQoJb3JpZ2lu...
   ```

4. Open the credentials file:

   ```powershell
   New-Item -ItemType Directory -Force "$HOME\.aws"
   notepad "$HOME\.aws\credentials"
   ```

5. Delete everything in it, paste the block, save, and close Notepad.
6. Check:

   ```powershell
   aws sts get-caller-identity
   ```

   The output must contain `assumed-role/voclabs`.

> **The file must be named `credentials`, with no extension.** If Notepad saved
> `credentials.txt`, Terraform fails with `No valid credential sources found`.
> Fix it with `Rename-Item "$HOME\.aws\credentials.txt" credentials`.

## Step 2 - Create the cluster

```powershell
cd terraform
terraform init      # first time only: downloads the AWS provider
terraform apply     # type "yes" when asked
```

- First creation takes about **15 minutes**.
- If the cluster already exists, `apply` answers **No changes** in seconds.
- If the lab removed it between sessions, `apply` simply recreates it.
- If the session expires during `apply`, redo Step 1 and run `apply` again.

Optional overrides (region, node count, instance type) go in
`terraform.tfvars` - copy [`terraform.tfvars.example`](terraform.tfvars.example).

## Step 3 - Connect kubectl and check the cluster

```powershell
aws eks update-kubeconfig --region us-east-1 --name achat-eks
kubectl get nodes
```

You should see 2 nodes with status `Ready`. The same command is printed by
`terraform output kubeconfig_command`; the Kubernetes version is shown by
`terraform output cluster_version`.

## Step 4 - Deploy the application with Jenkins

Follow [`../ci/README.md`](../ci/README.md#continuous-delivery-docker-hub-and-eks):
create the deploy credentials once, then run the pipeline with
`DEPLOY_K8S` checked. The end of the `DEPLOY TO EKS` log prints:

```text
Application: http://xxxxxxxx.us-east-1.elb.amazonaws.com
```

Open it in a browser. The load balancer DNS name can take 2-3 minutes to work.

Check from your PC at any time:

```powershell
kubectl -n achat get pods,svc
```

The application and monitoring pods (`mysql`, `backend`, `frontend`,
`grafana-lgtm`, `alloy`) must be `Running` and `READY 1/1`, and the `frontend`
service must have an `EXTERNAL-IP`. Open Grafana with
`kubectl -n achat port-forward svc/grafana-lgtm 3001:3000`, then visit
<http://localhost:3001>. See [`../observability/README.md`](../observability/README.md).

### Manual deploy (without Jenkins)

Only if Jenkins is not available, once the images are on Docker Hub:

```powershell
kubectl apply -f k8s/namespace.yaml
kubectl -n achat create secret generic achat-db `
  --from-literal=MYSQL_ROOT_PASSWORD=<random> `
  --from-literal=DB_USERNAME=achat_user `
  --from-literal=DB_PASSWORD=<another-random>
# First replace DOCKERHUB_USER in k8s/backend.yaml and k8s/frontend.yaml.
kubectl apply -f k8s/
kubectl -n achat get svc frontend
```

## Step 5 - Clean up after every demo

```powershell
kubectl delete namespace achat
terraform destroy      # type "yes"
```

Delete the namespace **first**: the load balancer is created by Kubernetes, not
by Terraform, and would keep costing money and block the cleanup.

## Demo-day checklist

1. Start Lab → copy credentials (Step 1).
2. `terraform apply` (Step 2) and `kubectl get nodes` (Step 3).
3. Upload the new credentials file to the Jenkins `aws-learner-lab` credential.
4. Run Jenkins with `DEPLOY_K8S` checked and open the application URL.
5. Show Git history, Jenkins, SonarQube, Nexus, Docker Hub and the running app.
6. `kubectl delete namespace achat` then `terraform destroy`.

## Troubleshooting

| Message | Cause | Fix |
| --- | --- | --- |
| `No valid credential sources found` | Credentials file missing or named `credentials.txt` | Step 1 |
| `ExpiredToken` / `voc-cancel-cred` | Lab session ended | Start Lab, redo Step 1 |
| `kubectl ... localhost:8080 refused` | kubectl not connected to the cluster | Step 3 |
| `kubectl ... no such host` | Cluster deleted or lab stopped | Start Lab, then Step 2 and Step 3 |
| `frontend` has no `EXTERNAL-IP` | Load balancer still being created | Wait 2-3 minutes |

## Notes

- State is local (`terraform.tfstate`, git-ignored). Run Terraform from the
  same PC each time, or the cluster will look unknown to Terraform.
- MySQL uses `emptyDir` storage (the lab cannot give the EBS driver its IAM
  role). Data is lost when the pod restarts; demo data is seeded again.
- The service CIDR is fixed to `172.20.0.0/16`, so the cluster DNS is
  `172.20.0.10`, used by the frontend nginx as `DNS_RESOLVER`.
