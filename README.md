# Automatic Exchange of information frontend

This microservice automatically routes users depending on enrolment credentials to a frontend service whereby they can register for and/or submit cross border arrangements.

Enrolment details for MDR: 

      enrolmentKey = "HMRC-MDR-ORG"  
      identifier = "MDRID"  
      registrationUrl = "/register-for-exchange-of-information/mdr"
      fileUploadUrl = "/report-under-mandatory-disclosure-rules"
  
  Enrolment details for DAC6: 

     enrolmentKey = "HMRC-DAC6-ORG"  
     identifier = "DAC6ID"
     registrationUrl = "/register-for-cross-border-arrangements"
     fileUploadUrl = "/disclose-cross-border-arrangements/upload"



## Run Locally


Run the following command to start services for MDR locally:

    sm --start MDR_ALL -f
    
Run the following command to start services for DAC6 locally:

    sm --start DAC6_ALL -f
    
Automatic Exchange of information frontend runs on port 10021

## Requirements

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), and requires a Java 8 [JRE] to run.


### License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
