# JDAC Spring Boot Starter

JDAC Spring Boot Starter is a Spring Boot wrapper of 
[Java Data Access client](https://github.com/bklogic/java-data-access-client). 
It streamlines data access layer development backed by data access services, and reduces the 
data access layer (aka repository layer) of Spring Boot Application into a thin layer 
of interfaces annotated with `@QueryService`, `@CommandService` and `@RepositoryService`, 
which serve as proxies of backend data access services.

If you don't know what data access service is, please take a look at:

[Data Access Service Documentation](https://docs.backlogic.net/#/DataAccessService)

It is a simple way to solve complex relational database access problem.

To get started with this JDAC Spring Boot Starter, please read on.

## Get Started

### Maven Dependency

```xml
<dependency>
    <groupId>net.backlogic.persistence</groupId>
    <artifactId>jdac-spring-boot-starter</artifactId>
    <version>0.0.5</version>
</dependency>
```

### Configuration in `application.yml`

```yaml
jdac:
    baseUrl: https://try4.devtime.tryprod.backlogic.net/service/try4/example
    basePackage: net.backlogic.persistence.springboot.classic.repository
    logRequest: true
    jwtProvider:
        class: simple
        jwt: ""
```

- baseUrl - baseUrl of backend data access application
- basePackage - root package to start data access interface scanning
- logRequest - enable request and response logging if true
- jwtProvider - specifying a JwtProvider supplying JWT bearer token, if needed

#### JwtProvider

A JwtProvider is required when the service request needs to be sent with a JWT token, as in the case with
DevTime service hosted in BackLogic workspace. 

There are two built-in JwtProviders: simple and basic. They can be configured as follows, respectively:

##### Simple

```yml
    jwtProvider:
        class: simple
        jwt: token-string-comes-here
```

The simple JwtProvider is simply configured with a valid JWT token. This provider is meant for quick and short test
of JDAC applications.

##### Basic

```yml
    jwtProvider:
        class: basic
        authEndpoint: https://try4.devtime.tryprod.backlogic.net/service/try4/auth
        serviceKey: key-string
        serviceSecret: secret-string
```

The basic JwtProvider relies on an auth service for JWT token. Therefore, it is configured with three parameters:
- authEndpoint - endpoint of auth service
- serviceKey and serviceSecret - credential for the auth service

The embedded JDAC client will send the auth service a request in format of:

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

where `expiryTime` is milliseconds since epoch.  

The `authEndpoint`, `serviceKey` and `serviceSecret` for your BackLogic workspace is available from *Service Builder*.

### Data Access (aka Repository) Interfaces

#### Query Interface

```java
@QueryService("myQueries")
public interface MyQuery {
    @Query("getCustomerByCustomerNumber")
    Customer getCustomer(String customerNumber);
    
    @Query("getCustomersByPostalCode")
    List<Customer> getCustomersByPostalCode(String postalCode);
}
```

Mapped to query services `myQueries/getCustomerByCustomerNumber` and `myQueries/getCustomersByPostalCode`, 
respectively, in backend data access application. One or more queries may be specified in a `@QueryService` interface.

#### Command Interface

``` java
@CommandService("myCommands")
public interface MyCommand {
	@Command("deleteCustomer")
	void removeCustomer(Integer customerNumber);
}
```

Mapped to SQL (command) service `myCommands/deleteCustomer` in backend data access application. 
One or more commands may be specified in a `@CommandService` interface.

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
One `@RepositoryService` interface per CRUD service.


### Service Class

```groovy
public class MyService {
    @Autowired
    MyQuery myQuery;
    
    @Autowired
    MyCommand myCommand;

    @Autowired
    CustomerRepository customerRepository;
    
    public void playWithDataServices() {
        // query
        List<Customer> customers = myQuery.getCustomersByPostalCode("90001");

        // command
        myQuery.removeCustomer (123);

        // repository
        Customer customer = new Customer("Joe", "Los Angeles", "90001");
        customer = repository.create(customer);
        customers = repository.getByCity("Los Angeles");
    }
}
```

Note how the `@QueryService`, `@CommandService` and `@RepositoryService` interfaces are autowired and called in the service class.

## Example Application

Lastly, an example JDAC Spring Boot application is available here:  

https://github.com/bklogic/jdac-spring-boot-example

for your reference.
