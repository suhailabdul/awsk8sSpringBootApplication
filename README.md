# How to Create a Kubernetes Cluster on AWS free Tier with Jenkins and Spring Boot Application?
In this Article , We will setup an AWS Environment to deploy Spring Boot Application in k8s Cluster with free tier EC2 Instance. Kubernetes can be installed on AWS(Amazon Web Services) as explained in the Kubernetes documentation either using conjure-up, Kubernetes Operations (kops), CoreOS Tectonic or kube-aws. Out of those options I found kops extremely easier to use and its nicely designed for customizing the installation, executing upgrades and managing the Kubernetes clusters over time. In this article I will explain how to use Kubernetes Operations tool to install a Kubernetes Cluster on AWS in few minutes.

Steps to Follow
1.	First we need an AWS account and access keys to start with. Login to your AWS console and generate access keys for your user by navigating to Users/Security credentials page.
2.	Create an EC2 Instance with t2.micro instance for Managing k8s Cluster
3.	Create a new IAM user or use an existing IAM user and grant following permissions to newly Created EC2 Instance:
## 
	AmazonEC2FullAccess
	AmazonRoute53FullAccess
	AmazonS3FullAccess
	AmazonVPCFullAccess
	AmazonIAMFullAccess
4.	Install AWS CLI by following its official installation guide:
##
	pip install awscli --upgrade –user
5.	Install kops by following its official installation guide:
## 
	curl -LO https://github.com/kubernetes/kops/releases/download/$(curl -s https://api.github.com/repos/kubernetes/kops/releases/latest | grep tag_name | cut -d '"' -f 4)/kops-linux-amd64
	chmod +x kops-linux-amd64
	sudo mv kops-linux-amd64 /usr/local/bin/kops
6.	Configure the AWS CLI by providing the Access Key, Secret Access Key and the AWS region that you want the Kubernetes cluster to be installed:
## 
	aws configure
	AWS Access Key ID [None]: 
	AWS Secret Access Key [None]: 
	Default region name [None]: ap-south-1b
	Default output format [None]:
7.	Create an AWS S3 bucket for kops to persist its state:
##
	bucket_name=dev.k8s.abdul.in
	aws s3api create-bucket --bucket ${bucket_name} --region ap-south-1b
8.	Enable versioning for the above S3 bucket:
##
	aws s3api put-bucket-versioning --bucket ${bucket_name} --versioning-configuration Status=Enabled
9.	Provide a name for the Kubernetes cluster and set the S3 bucket URL in the following environment variables:
##
	export KOPS_CLUSTER_NAME=dev.k8s.abdul.in
	export KOPS_STATE_STORE=s3://${bucket_name}
Add above code block can be added to the **~/.bash_profile** or **~/.profile** file depending on the operating system to make them available on all terminal environments.

10.	Create Private Hosted Zone as **dev.k8s.abdul.in** and enter VPN region as **ap-south-1b**, To Create Hosted Zone,  follow the steps [here](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/hosted-zone-private-creating.html).

11.	Create a Kubernetes cluster definition using kops by providing the required node count, node size, and AWS zones. The node size or rather the EC2 instance type would need to be decided according to the workload that you are planning to run on the Kubernetes cluster:
##
 	kops create cluster --node-count=2 --node-size=t2.micro --zones=ap-south-1b --name=${KOPS_CLUSTER_NAME} --master-size=t2.micro --master-count=1 --dns private
If you are seeing any authentication issues, try to set the following environment variables to let kops directly read EC2 credentials without using the AWS CLI:
##
	sshkeygen
	kops create secret --name dev.k8s.abdul.in sshpublickey admin -i ~/.ssh/id_rsa.pub
If needed execute the kops create cluster help command to find additional parameters:
##
	kops create cluster --help
 Review the Kubernetes cluster definition by executing the below command:
##
	kops edit cluster --name ${KOPS_CLUSTER_NAME}
12.	Now, let’s create the Kubernetes cluster on AWS by executing kops update command:
##
	kops update cluster --name ${KOPS_CLUSTER_NAME} --yes
13.	Above command may take some time to create the required infrastructure resources on AWS. Execute the validate command to check 	its status and wait until the cluster becomes ready:
##
	kops validate cluster 
Once the above process completes, kops will configure the Kubernetes CLI (kubectl) with Kubernetes cluster API endpoint and user credentials.

14.	Now, you may need to deploy the Kubernetes dashboard to access the cluster via its web based user interface:
##
	kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v1.10.1/src/deploy/recommended/kubernetes-dashboard.yaml
15.	Execute the below command to find the admin user’s password:
## 
	kops get secrets kube --type secret –o plaintext
16.	Execute the below command to find the Kubernetes master hostname:
##
	kubectl cluster-info 
17.	Access the Kubernetes dashboard  through proxy, type the following command:
##
	kubectl proxy --address 0.0.0.0 --accept-hosts '.*' &
Now Dashboard URL will be accessed like below:
	http://<AWSk8sManagementpublicIP>:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/#!/ 
	
## 18.	To Skip k8s Dashboard Login: 
	
	cat <<EOF | kubectl create -f -
	apiVersion: rbac.authorization.k8s.io/v1beta1
	kind: ClusterRoleBinding
	metadata:
	  name: kubernetes-dashboard
	  labels:
	    k8s-app: kubernetes-dashboard
	roleRef:
	  apiGroup: rbac.authorization.k8s.io
	  kind: ClusterRole
	  name: cluster-admin
	subjects:
	- kind: ServiceAccount
	  name: kubernetes-dashboard
	  namespace: kube-system
	EOF
Afterwards you can use skip option on login page to access Dashboard.
	If you are using dashboard version v1.10.1 or later, you must also add **--enable-skip-login** to the deployment's command line arguments. You can do so by adding it to the args in 
## 
	kubectl edit deployment/kubernetes-dashboard --namespace=kube-system.
## Example:
     containers:
      - args:
        - --auto-generate-certificates
        - --enable-skip-login            # <-- add this line
        image: k8s.gcr.io/kubernetes-dashboard-amd64:v1.10.1
19.	Go to AWS Master and Nodes security group, click on InBound rules and add the following rule.
Port range to open in Master and Nodes Security Group: **30000-32767**
 
20.	Deploying Spring Boot Application via Jenkins
## Environment Setup:
## i.	Type the following command to install JAVA 8
	sudo yum install java-1.8.0-openjdk
	sudo yum install java-1.8.0-openjdk-devel
	sudo alternatives --config java
	sudo yum remove java-1.7*
## ii.	Install Jenkins in k8s Management Server 
	sudo wget -O /etc/yum.repos.d/jenkins.repo http://pkg.jenkins-ci.org/redhat/jenkins.repo
	sudo rpm --import http://pkg.jenkins-ci.org/redhat/jenkins-ci.org.key
	sudo yum install -y jenkins
	sudo service jenkins start
## iii.	To install Maven type the following
	wget https://www-eu.apache.org/dist/maven/maven-3/3.6.2/binaries/apache-maven-3.6.2-bin.tar.gz
	tar –xvf apache-maven-3.6.2-bin.tar.gz
	Go to Home Path /home/ec2-user
	vi .bash_profile
	Add Maven path in existing path variable and save.
	source ~/.bash_profile
## iv.	To install docker type the following command :
	sudo yum install -y docker
	sudo service docker start
	sudo usermod -aG docker ec2-user
	sudo usermod -aG docker Jenkins
## v.	To install git type the following command: 
	yum install –y git		
vi.	After above softwares installed, Create Admin User in Jenkins UI and installed Suggested Plugins.

vii.	Add JDK, Maven Path in Global Tool Configuration.

viii.	Go to Manage Jenkins -> Manage Plugins -> Available and install **Pipeline Utility Steps and Amazon ECR plugin**.

ix.	Go to Credentials and Add AWS Credentials and Enter your Access Key ID and Secret Access Key.  If you haven’t created AWS Access 	 Key ID and kindly follow the below [link](https://docs.aws.amazon.com/cli/latest/reference/iam/create-access-key.html)

x.	Go to Global System Environment , Create below two Environment Path:

		awsECRUrl  -> Enter your ECR URL If you can’t Created ECR, kindly follow the below [link](https://docs.aws.amazon.com/AmazonECR/latest/userguide/repository-create.html)
		
		awsID - > ecr:<regionname>:<aws credentialsid> ,Eg : ecr:ap-south-1:f990151f-44cf-4adc-971a-457629978f9b( Jenkins AWS Credentials ID get from ID column which you have added before).
	
xi.	Now Create a Pipeline Job and Add GitHub Project URL and enable GitHub hook trigger for GITScm polling, select Pipeline script 		from SCM -> SCM -> Git -> Enter Repository URL -> Click Save -> Apply.

## xii.	Before triggering  job and enter the below commands to execute k8s Script with Jenkins User: 
	mkdir -p /var/lib/jenkins/.kube
	sudo cp -i /root/.kube/config /var/lib/jenkins/.kube/config
	sudo chown  jenkins:jenkins –R /var/lib/jenkins/.kube/config
	sudo chown  Jenkins:jenkins -R /var/lib/jenkins/.kube/
xiii.	Once Job started, It will ask to enter AWS Master IP to patching Node Port.

xiv.	Once Job completed, in Console Logs you can see the Application URL.

## 21.	How to shut down a kops Kubernetes cluster on AWS?
	kops edit ig nodes and set maxSize and minSize to 0.
	To Shutdown master node, Need to add master name in Command, To get to know name of master and type the following.: kops get ig
	kops edit ig <MasterNodeName> and again set maxSize and minSize to 0
	Finally, Need  to update cluster, enter kops update cluster --yes and then kops rolling-update cluster
	Awesome, cluster is offline now! no need to go into AWS. If you wanted to turn your cluster back on, revert the settings, changing your master to at least 1, and your nodes to 2.
