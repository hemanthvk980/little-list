package in.littlelist;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(org.springframework.dao.TransientDataAccessException.class)
    ResponseEntity<ProblemDetail> transientDatabaseFailure() {
        return ResponseEntity.status(503).header("Retry-After", "2")
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "This gift is busy. Please retry in a moment using the same confirmation link."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "Check the highlighted fields and try again.");
        problem.setProperty("fields", fields);
        problem.setProperty("code", "INVALID_FIELDS");
        return problem;
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> serviceError(ResponseStatusException ex) {
        ProblemDetail problem = ex.getBody();
        problem.setProperty("code", switch (ex.getStatusCode().value()) {
            case 404 -> "UNAVAILABLE";
            case 409 -> "STATE_CONFLICT";
            case 410 -> "CLAIM_EXPIRED";
            case 429 -> "RATE_LIMITED";
            case 503 -> "MAIL_UNAVAILABLE";
            default -> "INVALID_REQUEST";
        });
        ResponseEntity.BodyBuilder response = ResponseEntity.status(ex.getStatusCode());
        if (ex.getStatusCode().value() == 429) response.header("Retry-After", "900");
        return response.contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }
}
