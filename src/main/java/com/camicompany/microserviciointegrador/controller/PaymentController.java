
package com.camicompany.microserviciointegrador.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.CreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.weebhookDto.HelipagosWebhookRequest;
import com.camicompany.microserviciointegrador.dto.PaymentResponse;
import com.camicompany.microserviciointegrador.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Create a new payment")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error: an unexpected error occurred in the integrator service", content = @Content),
        @ApiResponse(responseCode = "503", description = "Service unavailable: Helipagos is unreachable", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse paymentResponse = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentResponse);
    }

    @Operation(summary = "Get payment details by external identifier (idSp)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found", content = @Content),
        @ApiResponse(responseCode = "503", description = "Service unavailable: Helipagos is unreachable", content = @Content)
    })
    @GetMapping("/{idSp}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String idSp) {
        PaymentResponse paymentResponse = paymentService.getPayment(idSp);
        return ResponseEntity.ok(paymentResponse);
    }

    @Operation(summary = "Cancel a payment by external identifier (idSp)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment cancelled",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Cannot cancel payment in the current state", content = @Content),
        @ApiResponse(responseCode = "503", description = "Service unavailable: Helipagos is unreachable", content = @Content)
    })
    @DeleteMapping("/{idSp}")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable String idSp) {
        PaymentResponse response = paymentService.cancelPayment(idSp);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Receive Helipagos webhook notifications to update payment status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Webhook processed successfully", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid API Key or invalid data", content = @Content)
    })
    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(@RequestBody HelipagosWebhookRequest request, @RequestHeader("api-key") String apiKey) {

        paymentService.processWebhook(request, apiKey);
        return ResponseEntity.ok().build();
    }

}
