### Deploy
* Is there a problem in using maven for LoadBalancer and AutoScaler, because they should be
deployed on the same instance.

### LoadBalancer
* Can we consider the solution for a certain request is the same no matter the
algorithm or do we need to take the algorithm into account when accessing the caches?
* Can we decide not to repeat requests with the same query as one request being processed?
But then we need to have another structure that maps a query to all the client requests
pending for that request. Should we map the processing requests with an UUID or 
by their query, since the query can be repeated?
