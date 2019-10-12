import groovy.json.JsonSlurper
node {
    
             env.PATH = "${tool 'Maven'}/bin:/usr/local/bin:${env.PATH}"
	

   stage('Clone Repository') {
        // Get some code from a GitHub repository
        git 'https://github.com/suhailabdul/springbootecommerce.git'
    
   }
   stage('Build Maven Image') {
       def pom = readMavenPom file: 'pom.xml'
        echo "${pom}"
        sh 'mvn clean install -Dmaven.test.skip=true'
   }
   
   stage('Docker Build Image') {
       def dimage = docker.image("springbootecommerce")
       echo "Dockage image exists:: ${dimage.id}"
        if (env.BRANCH_NAME == 'springbootecommerce') {
            sh "docker rmi ${dimage.id}"
        }
        docker.build("springbootecommerce")  
   }
    
    stage ('Docker push') {
	    docker.withRegistry("${env.awsECRUrl}", "${env.awsID}") {
        docker.image('springbootecommerce').push('latest')
        }
     }
    
  /*  stage('Deploy Application') {
     //  sh 'nohup docker-compose up  >> docker.out &'
     sh '''
         if [ -z `docker-compose ps -q springbootecommerce` ] || [ -z `docker ps -q --no-trunc | grep $(docker-compose ps -q springbootecommerce)` ]; then
             echo "No, it's not running."
              docker-compose up -d
        else
             echo "Yes, it's running."
              docker-compose down  
              docker-compose up -d
        fi
        echo "Sleeping for 10 Seconds"
        sleep 10
        echo "Done"
        '''
         
       // sh 'tail -f docker.out | sed "/^Started$/ q"'   
     
   }*/
    
      stage('Post Action') {
        
        echo 'Clearing Null Images'
            sh (script:'docker rmi $(docker images --filter "dangling=true" -q --no-trunc) 2>/dev/null', returnStatus:true)   
         
        
    }
    


   
    stage('Deploy k8s Services') {
            sh '''
                export KUBECONFIG=~/.kube/config
               kubectl apply -f k8s-springbootecommerce-service.yml
             '''  
    }
    
     stage('Deploy k8s Application') {
          sh '''
              
              export KUBECONFIG=~/.kube/config
               kubectl apply -f k8s-springbootecommerce-deployment.yml
             ''' 
    }
    
    stage('Patching External IP') {
        def MasterIP = input(
                            id: 'userInput', message: 'Enter Master Node External IP Address',
                            parameters: [

                                    string(defaultValue: 'None',
                                            description: 'Master Node External IP Address',
                                            name: 'MasterIP'),
                                    
                            ])
                    echo("Master IP Address: ${MasterIP}")
            sh (script : $/ kubectl patch svc springbootecommerce -p '{"spec":{"externalIPs":["${MasterIP}"]}}' /$)
                def nodeport = getNodePortwithK8s()
        echo "Your services are running in http://${MasterIP}:${nodeport}"
    }
    
   
}

   def waitForServices() {
  sh "kubectl get svc -o json > services.json"
 
  while(!toServiceMap(readFile('services.json')).containsKey('springbootecommerce')) {
        sleep(10)
        echo "Services are not yet ready, waiting 10 seconds"
        sh "kubectl get svc -o json > services.json"
  }
  echo "Services are ready, continuing"
}
 
@com.cloudbees.groovy.cps.NonCPS
Map toServiceMap(servicesJson) {
  def json = new JsonSlurper().parseText(servicesJson)
 
  def serviceMap = [:]
  json.items.each { i ->
    def serviceName = i.metadata.name
    def ingress = i.status.loadBalancer.ingress
    if(ingress != null) {
      def serviceUrl = ingress[0].hostname
      serviceMap.put(serviceName, serviceUrl)
    }
  }
 
  return serviceMap
}

def String getNodePortwithK8s() {
  sh "kubectl get svc -o json > services.json"
 return getNodePort(readFile('services.json'))
}

@com.cloudbees.groovy.cps.NonCPS
String getNodePort(servicesJson) {
  def json = new JsonSlurper().parseText(servicesJson)
 
  def nodeport
  json.items.each { i ->
    def serviceName = i.metadata.name
	if (serviceName == "springbootecommerce") {
		nodeport = i.spec.ports[0].nodePort
		if(nodeport != null) {
		    echo "$nodeport"
    }
	}
    
  }
 
  return nodeport
}
