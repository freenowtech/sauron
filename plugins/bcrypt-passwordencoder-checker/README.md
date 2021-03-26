# Bcrypt Password Encoder Checker Plugin




## Description

BcryptPasswordEncoder Checker is responsible for checking whether a service is instantiating BcryptPasswordEncoder in the directories containing configuration for the service 
(usually `config`). 



## Motivation
Bcrypt is an algorithm used for hashing passwords. It is by design a slow algorithm, which is good for preventing brute force attacks , and usually used for hashing 
user 
passwords during authentication.  

The same thing that makes it good for user authentication, works the opposite for server to server communication. The fact that it is a slow algorithm can penalize the 
latency when used with http basic authentication across services. Provided the connection is secured by https , the usage of this algorithm is discouraged.  


## Configuration

It is possible to specify the list of directories considered as configuration, following this format: 

```yaml
sauron.plugins:
  bcrypt-passwordencoder-checker:
    config-directories:
      -   
         name: <configDirectoryName1>
      -
         name: <configDirectoryName2>
```    

This parameter is optional, meaning that if not provided it will default to just search in directories named as `config`

## Input

* repositoryPath: Path to the source code project

## Output
* `encodesPasswordsWithBcrypt`: Boolean that describes whether a project might be encoding passwords using Bcypt algorithm.
