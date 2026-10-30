export AWS_PAGER=""
export AWS_ACCESS_KEY_ID=
export AWS_SECRET_ACCESS_KEY=
export AWS_SESSION_TOKEN=''
export AWS_DEFAULT_REGION=us-east-1
export KUBECONFIG=/var/jenkins_home/workspace/pipeline-reglement-ines/.kube/config

# Vérifie les identifiants
aws sts get-caller-identity

# 1. Supprime l'application (et le LoadBalancer AWS)
kubectl delete namespace achat --timeout=5m
sleep 60
aws elb describe-load-balancers --query "LoadBalancerDescriptions[].LoadBalancerName"

# 2. Détruit le cluster EKS et le node group
cd /var/jenkins_home/workspace/pipeline-reglement-ines/terraform
terraform destroy -auto-approve

# 3. Vérifications finales
aws eks list-clusters
aws elb describe-load-balancers --query "LoadBalancerDescriptions[].LoadBalancerName"
