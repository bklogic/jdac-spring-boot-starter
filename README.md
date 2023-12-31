# JDAC Spring Boot Starter

JDAC Spring Boot Starter is a Spring Boot wrapper of 
[Java Data Access client](https://github.com/bklogic/java-data-access-client). 
It streamlines data access layer development backed by data access services, and reduces the 
data access layer (aka repository layer) of Spring Boot Application into a thin layer 
of interfaces annotated with `@QueryService`, `@CommandService` and `@RepositoryService`, 
which serve as proxies of backend data access services.

If you don't know what data access service is, please take a look at:

[Data Access Service Documentation](https://www.backlogic.net/docs/concepts)

It is a simple way to solve complex relational database access problem.

To get started with this JDAC Spring Boot Starter, please read on.

## Get Started

### Maven Dependency

```xml
<dependency>
    <groupId>net.backlogic.persistence</groupId>
    <artifactId>jdac-spring-boot-starter</artifactId>
    <version>0.1.7</version>
</dependency>
```
> Note: If you have `spring-devtools` in your pom, it needs to be disabled. Otherwise, it will cause the 
> BeanNotFound problem for user-defined data access service interface, 
> as the proxy for the interface produced by the starter is loaded with base classloader, while the interface itself is 
> loaded with the restart classloader. One possible solution for this problem is to include the JDAC jars with the
> restart classloader, as described in the 
> [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/3.1.5/reference/html/using.html#using.devtools.restart.customizing-the-classload),
> but I have not been able to make it work for Spring Boot 3.1.5.

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

## Advanced

### Batch Service (Experimental)

Batch service is an experimental feature. It provides a way to batch multiple queries for less-related data, 
and a way to expand the transaction boundary of write operations. The batch commands and repository operations
are executed in a single DB transaction, in the order that they are called.

The are three components to batch service, as given below.

#### Batch DTO

For accept of result of batch service. It should a no-arg constructor and standard getter and setter methods 
that are omitted here for brevity.

```java
public class BatchDTO {
    private Customer customer;
    private List<Employee> employees;
}
```

#### Batch Service Interface

- Annotated with @BatchService. 
- Must extend the generic Batch<T> interface, and use T to indicate return type.
- Use @Query, @Command, @Create, @Save, etc. to add query, command or 
repository operations to the batch.
- Use @ReturnMapping to map the service output to a member field of batch DTO

```java
@BatchService("")
public interface BatchQuery extends Batch<BatchDTO> {
	@Query("query/getCustomerByCustomerNumber")
	@ReturnMapping("customer")
	Customer getCustomer(int customerNumber);

	@Query("query/listEmployees")
	@ReturnMapping("employees")
	List<Employee> getEmployees();
}
```

#### Batch Service Call

- Use DataAccessClient to get a BatchService proxy. Batch proxy is stateful;
- Call service methods to add service to the batch;
- Call get(), run() or save() method to initiate the batch service. These methods are inherited 
from the Batch interface, and are the same except in semantics.

```java
public class BatchExampleService {
    @Autowired
    DataAccessClient client;

    public BatchDTO batchedCustomerAndEmployees(int customerNumber) {
        BatchQuery batchQuery = client.getBatch(BatchQuery.class);
        batchQuery.getCustomer(customerNumber);
        batchQuery.getEmployees();
        BatchDTO dto = batchQuery.get();
        return dto;
    }
}
```

## Example Application

Lastly, an example JDAC Spring Boot application is available here:  

https://github.com/bklogic/jdac-spring-boot-example

for your reference.
