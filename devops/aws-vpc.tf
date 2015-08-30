module "vpc" {
  source = "github.com/joescii/aws-terraform-modules/vpc-2-zones"
  
  vpc_name = "lift-jetty-cluster"
  ec2_key_name = "${var.ec2_key_name}"
  region = "${var.region}"
  zone_A = "${module.region.zone_A}"
  zone_B = "${module.region.zone_B}"
  nat_ami = "${module.region.nat_ami}"
  bastion_ami = "${module.region.bastion_ami}"
}