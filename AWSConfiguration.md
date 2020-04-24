# AWS Configuration

Step-by-step configuration of the AWS Load Balancer and Auto Scaler, assuming the AMI is lready configuring. 

## Load Balancer

### Basic Configuration

* Enable advanced VPC configuration.

* Listener Configuration:

Load Balancer Protocol | Load Balancer Port | Instance Protocol | Instance Port
---------------------- | ------------------ | ----------------- | -------------
HTTP | 80 | HTTP | 8000

* Select us-east-1a subnet.

### Assign Security Groups

* Select an existing security group (CNV-ssh+http)

### Configure Health Check

* Ping Protocol: HTTP
* Ping Port: 8000
* Ping path: /ping

* Response Timeout: 5 seconds
* Interval: 30 seconds
* Unhealthy threshold: 2
* Healthy threshold: 10 

### Add EC2 Instances

Don't add any EC2 instances, they will be created automatically by the Auto Scaler.

* Enable Connection Draining: 300 seconds

### Attributes

* Idle Timeout: 400


## Launch Configurations 

Select project AMI, Free tier elegible machine.

### Configure Details

Give a name, and enable CloudWatch detailed monitoring.

### Add Storage

Select Default storage.

### Configure Security Group

* Select an existing security group (CNV-ssh+http)


## Auto Scaling Group

Select the previous Launch configuration.

### Configure Details

* Give a name.
* Start with 1 instance.
* Select us-east-1a subnet.

* Select "Receive traffic from one or more load balancers".
* Select the preciously created Load Balancer.
* Health Check Type: ELB
* Health Check Grace Period: 300 seconds
* Enable CloudWatch detailed monitoring.

### Scaling Policies

* Scale between 1 and 5 instances.
* Scale the Auto Scaling group using step or simple scaling Policies

#### Increase Group Size
Whenever average of CPU utilization is >= 80% for at least consecutive period of 1 minute, add 1 capacity unit.
And then wait 300 seconds before allowing another scaling activity.

#### Decrease Group Size
Whenever average of CPU utilization is < 20% for at least consecutive period of 5 minutes, remove 1 capacity unit.
And then wait 300 seconds before allowing another scaling activity.




## Authors

* **Eduardo Saldanha - 83453** 
* **Daniela Lopes - 86403** 
* **Francisco Matos - 86415** 

