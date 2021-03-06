package com.bitmoi.order.handler;

import com.bitmoi.order.domain.Orderbook;
import com.bitmoi.order.domain.Wallet;
import com.bitmoi.order.kafka.KafkaProducerService;
import com.bitmoi.order.service.OrderService;
import com.bitmoi.order.service.WalletService;
import com.bitmoi.order.util.JwtDecode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
@RequiredArgsConstructor
public class OrderHandler {

    private final Logger logger = LoggerFactory.getLogger(OrderHandler.class);

    private final OrderService orderService;
    private final WalletService walletService;
    private final KafkaProducerService kafkaProducerService;

    // 매매 주문 목록
    public Mono<ServerResponse> getOrderList(ServerRequest request) {
        logger.info("매매 주문 목록");
        Flux<Orderbook> orderFlux = orderService.getOrderList();
        return ok()
                .contentType(APPLICATION_JSON)
                .body(orderFlux, Orderbook.class)
                .log("getOrderbookList ok --------- ");
    }

    // 매매 주문하기
    public Mono<ServerResponse> orderBidnAsk(ServerRequest request) {
        logger.info("매매 주문하기");

        Mono<Orderbook> orderMono = request.bodyToMono(Orderbook.class)
                .flatMap(user -> {
                    return orderService.saveUserid(user, request);
                })
                .flatMap(orderbook -> orderService.orderBidnAsk(orderbook))
                .flatMap(orderbook -> {
                    System.out.println("orderbook > "+orderbook);
                    return walletQuan(orderbook);
                })
                .subscribeOn(Schedulers.parallel())
                .doOnSuccess(res -> kafkaProducerService.sendOrderMessage(res));

        return ok()
                .contentType(APPLICATION_JSON)
                .body(orderMono, Object.class)
                .onErrorResume(error -> ServerResponse.badRequest().build())
                .log("orderBidnAsk ok --------- ");
    }


    //주문 취소
    public Mono<ServerResponse> OrderCancel(ServerRequest request) {
        logger.info("주문 취소");
        Integer orderid = Integer.valueOf(request.pathVariable("orderbookid"));
        Mono<Orderbook> orderMono = orderService.OrderCancel(orderid)
                .subscribeOn(Schedulers.parallel())
                .doOnSuccess(res -> kafkaProducerService.sendOrderMessage(res));

        return ok()
                .contentType(APPLICATION_JSON)
                .body(orderMono, Orderbook.class)
                .onErrorResume(error -> ServerResponse.badRequest().build())
                .log("getOrderId ok --------- ");
    }


    private Mono<Orderbook> walletQuan(Orderbook orderbook) {
        logger.info("지갑 Waiting_qty 업데이트");
        System.out.println("######q userid > "+String.valueOf(orderbook.getUserid()) + " , coinid " + String.valueOf(orderbook.getCoinid()));
        return (Mono<Orderbook>) walletService.getWallet(orderbook.getUserid(), orderbook.getCoinid())
                .flatMap(n -> {
                    int val = n.getQuantity().subtract(n.getWaiting_qty()).compareTo(orderbook.getQuantity().multiply(orderbook.getPrice()));

                    BigDecimal wal = n.getQuantity().subtract(n.getWaiting_qty());
                    BigDecimal order_quan = orderbook.getQuantity().multiply(orderbook.getPrice());
                    BigDecimal wallet_qty = n.getWaiting_qty().add(order_quan);
                    System.out.println("wal > " + wal + " ,order_quan >  " + order_quan + " , wallet_qty > " + wallet_qty);

                    if (val >= 0) { //order quantity가 더 적은 경우 (주문 전송)
                        n.setWaiting_qty(wallet_qty);
                        return walletService.updateWaitQuantity(n);
                    }
                    //주문 거절됨
                    orderbook.setState("reject");
                    return orderService.orderBidnAsk(orderbook);
                })
                .map(m -> orderbook);
    }


    //사용자의 매매 목록
    public Mono<ServerResponse> getAllByTypes(ServerRequest request) {
        logger.info("사용자의 매매 목록");
        Flux<Orderbook> orderFlux = orderService.getAllByTypes(request);
        return ok()
                .contentType(APPLICATION_JSON)
                .body(orderFlux, Orderbook.class)
                .log("getAllByTypes ok --------- ");
    }

}
