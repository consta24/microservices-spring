package com.constams.orderservice.service;

import com.constams.orderservice.dto.InventoryResponse;
import com.constams.orderservice.dto.OrderLineItemsDto;
import com.constams.orderservice.dto.OrderRequest;
import com.constams.orderservice.model.Order;
import com.constams.orderservice.model.OrderLineItems;
import com.constams.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

	private final OrderRepository orderRepository;
	private final WebClient.Builder webClientBuilder;

	public void placeOrder(OrderRequest orderRequest) {
		Order order = new Order();
		order.setOrderNumber(UUID.randomUUID().toString());

		if (orderRequest.getOrderLineItemsDtoList() == null) {
			throw new IllegalArgumentException("Order line items invalid");
		}

		List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
				.stream()
				.map(this::mapToOrderLineItems)
				.toList();
		order.setOrderLineItemsList(orderLineItems);

		List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();

		InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
				.uri("http://inventory-service/api/inventory", uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
				.retrieve()
				.bodyToMono(InventoryResponse[].class)
				.block();
		if (inventoryResponses == null) {
			throw new IllegalArgumentException("Inventory response invalid");
		}
		if (inventoryResponses.length == 0) {
			throw new IllegalArgumentException("Not valid products");
		}

		boolean allProductsInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);

		if (!allProductsInStock) {
			throw new IllegalArgumentException("Products not in stock");
		}
		orderRepository.save(order);
	}

	private OrderLineItems mapToOrderLineItems(OrderLineItemsDto orderLineItemsDto) {
		OrderLineItems orderLineItems = new OrderLineItems();
		orderLineItems.setPrice(orderLineItemsDto.getPrice());
		orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
		orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
		return orderLineItems;
	}
}
