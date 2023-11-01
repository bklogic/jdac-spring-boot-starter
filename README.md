# RDA Spring Boot Starter

RDAS, or relational data access, Spring Boot Starter simplifies development of the data access layer backed by 
data access services. It reduces the data access or repository layer of Spring Boot Application to a thin layer 
of interfaces annotated with @QueryService, @CommandService and @RepositoryService, which serves as a proxy 
of backend data access services.

To understand what data access service is, please take a look :  
[Data Access Service Documentation](https://docs.backlogic.net/#/DataAccessService)  
It is a simple way to to solve your complex relational database access problem.

To get started with this RDAS Spring Boot Starter, please read on.

## Get Started

### Maven Dependency

```xml
<dependency>
    <groupId>net.backlogic.persistence</groupId>
    <artifactId>rdas-spring-boot-starter</artifactId>
    <version>0.0.5</version>
</dependency>
```


### Configuration

```yaml
rdas:
  baseUrl: https://try4.devtime.tryprod.backlogic.net/service/try4/example
  basePackage: net.backlogic.persistence.springboot.classic.repository
  logRequest: true
  jwtProvider:
    class: simple
    jwt: ""
```

- baseUrl - baseUrl of a backend data access application
- basePackage - root package to start data access interface scanning
- logRequest - enable request and response logging if true
- jwtProvide - specifying a JwtProvider supplying JWT bearer token.

#### JwtProvider

A JwtProvider is required when the service request needs to sent with a JWT token, such as the case with
the DevTime service hosted in the BackLogic workspace. 

There are two built-in JwtProviders: simple and basic. They can be configured as follows, respectively:

##### Simple

```yml
    jwtProvider:
        class: simple
        jwt: ""
```

The simple JwtProvider is simply configured with a valid JWT token. This provider is good for a quick and short test
of your application.

##### Basic

```yml
    jwtProvider:
        class: basic
        authEndpoint: https://try4.devtime.tryprod.backlogic.net/service/try4/auth
        serviceKey: ""
        serviceSecret: ""
```

The basic JwtProvider relies on an auth service for JWT token. Therefore, it is configured with three parameters:
- authEndpoint - endpoint of auth service
- serviceKey and serviceSecret - credential for the auth service

The Spring Boot Application will send the auth service a request in format of:

```json
{
  "serviceKey": "my key",
  "serviceSecret": "my secret"
}
```

and expect a response in format of:

```json
{
  "jwt": "token text",
  "expiryTime": 1698797369479
}
```

The `authEndpoint`, `serviceKey` and `serviceSecret` for your BackLogic workspace is available from Service Builder.

### Data Access (aka Repository) Interfaces

#### Query Interface

```java
@QueryService("myQueries")
public interface MyQuery {
	@Query("getCustomers")
	List<Customer> getCustomersByCity(String city);
}
```

Mapped to query service `myQueries/getCustomers` in backend data access application.

#### Command Interface

``` java
@CommandService("myCommands")
public interface MyCommand {
	@Command("removeCustomer")
	void removeCustomer(Integer customerNumber);
}
```

Mapped to SQL (command) service `myCommands/removeCustomer` in backend data access application.

#### Repository Interface

``` java
@RepositoryService("myRepositories/Customer")
public interface CustomerRepository {
	@Create
	Customer create(Customer customer);
	@Update
	Customer update(Customer customer);
	@Delete
	Customer delete(Customer customer);
	@Read
	Customer getById(Integer customerNumber);
	@Read
	List<Customer> getByCity(String city);
}
```

Mapped to CRUD (repository) service `myRepositories/Customer` in backend data access application.


### Service Class

```groovy
public class MyService {
    @Autowired
    MyQuery myQuery;
    
    @Autowired
    MCommand myCommand;

    @Autowired
    CustomerRepository customerRepository;
    
    public void playWithDataServices() {
        // query
        List<Customer> customers = myQuery.getCustomersByCity("Los Angeles")

        // command
        myQuery.removeCustomer (123);

        // repository
        Customer customer = new Customer("Joe", "Los Angeles");
        customer = repository.create(customer)
    }
}
```

### Example Application

An example RDAS Spring Boot application is available here:  

https://github.com/bklogic/rdas-spring-boot-example

