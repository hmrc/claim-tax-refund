# Claim tax refund backend

CTR backend microservice.

### Dependencies

|Service|Link|
|-|-|
|File upload frontend|https://github.com/hmrc/file-upload-frontend|
|File upload|https://github.com/hmrc/file-upload|
|Pdf generator|https://github.com/hmrc/pdf-generator-service|

## Running the service

Service Manager: CTR_ALL 

|Repositories|Link|
|------------|----|
|Frontend|https://github.com/hmrc/claim-tax-refund-frontend|
|Stub|https://github.com/hmrc/claim-tax-refund-stubs|
|Journey tests|https://github.com/hmrc/claim-tax-refund-journey-tests|

Routes
-------
Port: 9869

| *Url* | *Description* |
|-------|---------------|
| /submit | The frontend submits to this endpoint |
| /file-upload/callback | Callback endpoint for file upload |
