package io.supportops.controller;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
@RestControllerAdvice
public class ExceptionAdvice {
    private static final Logger log=LoggerFactory.getLogger(ExceptionAdvice.class);
    @ExceptionHandler(ResponseStatusException.class) ProblemDetail status(ResponseStatusException e){return ProblemDetail.forStatusAndDetail(e.getStatusCode(),e.getReason()==null?"Request failed":e.getReason());}
    @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class}) ProblemDetail invalid(Exception e){return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,e instanceof IllegalArgumentException?e.getMessage():"Invalid request fields");}
    @ExceptionHandler(AccessDeniedException.class) ProblemDetail forbidden(){return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,"Your role cannot perform this action");}
    @ExceptionHandler(Exception.class) ProblemDetail unexpected(Exception e){log.error("Request failed",e);return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,"Request failed; check the server trace");}
}
