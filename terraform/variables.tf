variable "aws_region" {
  description = "AWS region. The Learner Lab only allows us-east-1 and us-west-2."
  type        = string
  default     = "us-east-1"

  validation {
    condition     = contains(["us-east-1", "us-west-2"], var.aws_region)
    error_message = "The AWS Academy Learner Lab only allows us-east-1 or us-west-2."
  }
}

variable "cluster_name" {
  description = "EKS cluster name, also used by the Jenkins DEPLOY TO EKS stage."
  type        = string
  default     = "achat-eks"
}

variable "kubernetes_version" {
  description = "EKS Kubernetes version. null uses the EKS default, which stays in standard (cheaper) support."
  type        = string
  default     = null
}

variable "node_instance_type" {
  description = "Worker node instance type. t3.medium is the smallest size that fits MySQL, the backend and the frontend."
  type        = string
  default     = "t3.medium"
}

variable "node_desired_size" {
  description = "Number of worker nodes."
  type        = number
  default     = 2
}

variable "node_min_size" {
  description = "Minimum number of worker nodes."
  type        = number
  default     = 1
}

variable "node_max_size" {
  description = "Maximum number of worker nodes."
  type        = number
  default     = 3
}
