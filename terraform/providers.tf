# Credentials come from the default AWS profile (~/.aws/credentials), filled
# from the Learner Lab "AWS Details > AWS CLI" block. Never put them here.
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = "achat"
      ManagedBy = "terraform"
    }
  }
}
