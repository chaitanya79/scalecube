package io.scalecube.services;

import static io.scalecube.services.ServiceHeaders.service_method_of;
import static io.scalecube.services.ServiceHeaders.service_request_of;

import io.scalecube.cluster.Cluster;
import io.scalecube.transport.Message;

import java.util.Optional;

public class ServiceDispatcher {

  private final Cluster cluster;
  private final ServiceRegistry registry;

  /**
   * ServiceDispatcher constructor to listen on incoming network service request.
   * 
   * @param cluster instance to listen on events.
   * @param registry service registry instance for dispatching.
   */
  public ServiceDispatcher(Cluster cluster, ServiceRegistry registry) {
    this.cluster = cluster;
    this.registry = registry;


    // Start listen messages
    cluster.listen()
        .filter(message -> service_request_of(message) != null)
        .subscribe(this::onServiceRequest);
  }

  private void onServiceRequest(final Message request) {
    Optional<ServiceInstance> serviceInstance =
        registry.getLocalInstance(service_request_of(request), service_method_of(request));

    DispatchingFuture result = DispatchingFuture.from(cluster, request);
    try {
      if (serviceInstance.isPresent()) {
        result.complete(serviceInstance.get().invoke(request));
      } else {
        result.completeExceptionally(new IllegalStateException("Service instance is missing: " + request.qualifier()));
      }
    } catch (Exception ex) {
      result.completeExceptionally(ex);
    }
  }
}
