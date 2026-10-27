# Terraform: EKS cluster on the AWS Academy Learner Lab

This folder creates the Kubernetes cluster used for continuous delivery
(step 10): an **EKS** cluster with a managed node group, in the default VPC of
the AWS Academy **Learner Lab** account. Jenkins then pushes the images to
Docker Hub and deploys the manifests from [`../k8s`](../k8s) to it.

| Resource | Purpose |
| --- | --- |
| `aws_eks_cluster.achat` | Kubernetes control plane (`achat-eks`) |
| `aws_eks_node_group.achat` | 2 × `t3.medium` worker nodes |
| `aws_ec2_tag.elb_subnet` | Tags the subnets so the frontend `LoadBalancer` Service gets a public ELB |

## Learner Lab constraints

- **Regions:** only `us-east-1` and `us-west-2` (enforced by a variable validation).
- **No IAM creation:** the cluster and the nodes both use the existing `LabRole`.
  That is why the community `terraform-aws-modules/eks` module is not used.
- **Temporary credentials:** they change at every lab session (about 4 hours).
- **Budget ($50):** EKS costs about $0.10/h for the control plane plus about
  $0.04/h per node and the load balancer. The control plane keeps billing when
  the lab session ends, so **destroy the cluster after each demo**.
- **AZs:** EKS rejects `us-east-1e`, so only AZs `a`–`d` are used.

## Requirements

- Terraform ≥ 1.6, AWS CLI v2 and kubectl. On Windows:

  ```powershell
  winget install Hashicorp.Terraform Amazon.AWSCLI Kubernetes.kubectl
  ```

## 1. Give Terraform the lab credentials

1. Open the Learner Lab and click **Start Lab**. Wait for the green dot next to **AWS**.
2. Click **AWS Details** → **AWS CLI: Show**.
3. Copy the whole block (`aws_access_key_id`, `aws_secret_access_key`,
   `aws_session_token`) into `C:\Users\<you>\.aws\credentials` (Linux/macOS:
   `~/.aws/credentials`) under `[default]`, replacing the old values.
4. Check:

   ```bash
   aws sts get-caller-identity
   ```

   The ARN must contain `assumed-role/voclabs`.

Repeat steps 1–3 at every new lab session.

## 2. Create the cluster

```bash
cd terraform
terraform init
terraform plan
terraform apply
```

Creation takes about 15 minutes. Optional overrides go in `terraform.tfvars`
(copy [`terraform.tfvars.example`](terraform.tfvars.example)).

## 3. Connect kubectl

```bash
aws eks update-kubeconfig --region us-east-1 --name achat-eks
kubectl get nodes
```

The same command is printed by `terraform output kubeconfig_command`.

## 4. Deploy the application

Through Jenkins (recommended): run the pipeline with `DOCKER_PUSH=true`,
`DOCKERHUB_USER=<your Docker Hub user>` and `DEPLOY_K8S=true`. The credentials
the pipeline needs are listed in [`../ci/README.md`](../ci/README.md#continuous-delivery-docker-hub-and-eks).

By hand, once the images are on Docker Hub:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl -n achat create secret generic achat-db \
  --from-literal=MYSQL_ROOT_PASSWORD=<random> \
  --from-literal=DB_USERNAME=achat_user \
  --from-literal=DB_PASSWORD=<another-random>
# Replace DOCKERHUB_USER in k8s/backend.yaml and k8s/frontend.yaml first.
kubectl apply -f k8s/
kubectl -n achat get svc frontend
```

Open `http://<EXTERNAL-IP hostname>` from the `frontend` Service. The ELB DNS
name can take 2–3 minutes to resolve.

## 5. Clean up (every time)

```bash
kubectl delete namespace achat   # removes the load balancer first
terraform destroy
```

Delete the namespace before `terraform destroy`: the ELB is created by
Kubernetes, not Terraform, and would otherwise block the VPC cleanup and keep
costing money.

## Notes

- State is local (`terraform.tfstate`, git-ignored). It is enough for a single
  student account; a team would use an S3 backend.
- MySQL uses `emptyDir` storage: the lab cannot give the EBS CSI driver its IAM
  role. Data is lost when the pod restarts and the demo data is seeded again.
- The service CIDR is fixed to `172.20.0.0/16`, so the cluster DNS is
  `172.20.0.10`; the frontend nginx uses it as `DNS_RESOLVER`.
- If the lab session expires during `apply`, refresh the credentials and run
  `terraform apply` again; Terraform continues where it stopped.
