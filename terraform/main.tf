# =============================================================================
# EKS cluster for the AWS Academy Learner Lab
#
# The lab forbids creating IAM roles, so the cluster and the nodes both use the
# pre-created LabRole. The default VPC is used to keep the lab footprint small.
# =============================================================================

data "aws_iam_role" "lab" {
  name = "LabRole"
}

data "aws_vpc" "default" {
  default = true
}

# EKS does not support every AZ (us-east-1e is rejected), so keep a-d only.
data "aws_subnets" "eks" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  filter {
    name   = "availability-zone"
    values = [for az in ["a", "b", "c", "d"] : "${var.aws_region}${az}"]
  }
}

resource "aws_eks_cluster" "achat" {
  name     = var.cluster_name
  version  = var.kubernetes_version
  role_arn = data.aws_iam_role.lab.arn

  vpc_config {
    subnet_ids              = data.aws_subnets.eks.ids
    endpoint_public_access  = true
    endpoint_private_access = false
  }

  # Fixed service range so the cluster DNS is always 172.20.0.10, the value
  # used as DNS_RESOLVER by the frontend nginx (k8s/frontend.yaml).
  kubernetes_network_config {
    service_ipv4_cidr = "172.20.0.0/16"
  }

  # The lab user that runs Terraform (and Jenkins, with the same credentials)
  # becomes cluster admin without any extra IAM setup.
  access_config {
    authentication_mode                         = "API_AND_CONFIG_MAP"
    bootstrap_cluster_creator_admin_permissions = true
  }
}

resource "aws_eks_node_group" "achat" {
  cluster_name    = aws_eks_cluster.achat.name
  node_group_name = "${var.cluster_name}-nodes"
  node_role_arn   = data.aws_iam_role.lab.arn
  subnet_ids      = data.aws_subnets.eks.ids
  instance_types  = [var.node_instance_type]
  capacity_type   = "ON_DEMAND"

  scaling_config {
    desired_size = var.node_desired_size
    min_size     = var.node_min_size
    max_size     = var.node_max_size
  }

  update_config {
    max_unavailable = 1
  }
}

# Lets Kubernetes find public subnets for the frontend LoadBalancer Service.
resource "aws_ec2_tag" "elb_subnet" {
  for_each    = toset(data.aws_subnets.eks.ids)
  resource_id = each.value
  key         = "kubernetes.io/role/elb"
  value       = "1"
}
