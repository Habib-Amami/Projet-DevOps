output "cluster_name" {
  description = "EKS cluster name."
  value       = aws_eks_cluster.achat.name
}

output "cluster_endpoint" {
  description = "Kubernetes API endpoint."
  value       = aws_eks_cluster.achat.endpoint
}

output "cluster_version" {
  description = "Kubernetes version running on the cluster."
  value       = aws_eks_cluster.achat.version
}

output "region" {
  description = "AWS region of the cluster."
  value       = var.aws_region
}

output "kubeconfig_command" {
  description = "Command that adds the cluster to ~/.kube/config."
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.achat.name}"
}
