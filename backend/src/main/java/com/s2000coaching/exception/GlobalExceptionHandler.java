package com.s2000coaching.exception;

import com.s2000coaching.storage.PhotoStorageException;
import com.s2000coaching.storage.StoredFileNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiError(Instant.now(), 404, "Not Found", List.of(ex.getMessage())));
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiError(Instant.now(), 409, "Conflict", List.of(ex.getMessage())));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiError(Instant.now(), 400, "Bad Request", List.of(ex.getMessage())));
	}

	@ExceptionHandler(TooManyRequestsException.class)
	public ResponseEntity<ApiError> handleTooManyRequests(TooManyRequestsException ex) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.body(new ApiError(Instant.now(), 429, "Too Many Requests", List.of(ex.getMessage())));
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ApiError(Instant.now(), 401, "Unauthorized", List.of(ex.getMessage())));
	}

	@ExceptionHandler(EmailNotVerifiedException.class)
	public ResponseEntity<ApiError> handleEmailNotVerified(EmailNotVerifiedException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ApiError(Instant.now(), 403, "Forbidden", List.of(ex.getMessage())));
	}

	/**
	 * Two requests raced past the "is it taken?" check and the database's unique constraint
	 * stopped the second. Without this it would be a bare 500 for what is simply a conflict.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleIntegrityViolation(DataIntegrityViolationException ex) {
		log.warn("Veritabanı bütünlük ihlali", ex);
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiError(Instant.now(), 409, "Conflict",
						List.of("Bu kayıt zaten var ya da başka bir kayıtla çakışıyor. Sayfayı yenileyip tekrar deneyin.")));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
		List<String> messages = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.toList();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiError(Instant.now(), 400, "Bad Request", messages));
	}

	/**
	 * The row is there but its bytes are not — a half-restored backup, a wiped volume. 404
	 * rather than 500: nothing is going to fix itself on a retry, and the member needs to be
	 * told the photo is gone so they can re-upload it.
	 */
	@ExceptionHandler(StoredFileNotFoundException.class)
	public ResponseEntity<ApiError> handleMissingFile(StoredFileNotFoundException ex) {
		log.warn("Kaydı olan ama dosyası bulunamayan fotoğraf istendi", ex);
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiError(Instant.now(), 404, "Not Found",
						List.of("Fotoğrafın dosyası sunucuda bulunamadı.")));
	}

	/** Storage itself failed: a full disk, a permission error, an unreachable bucket. */
	@ExceptionHandler(PhotoStorageException.class)
	public ResponseEntity<ApiError> handleStorageFailure(PhotoStorageException ex) {
		log.error("Fotoğraf deposu hata verdi", ex);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ApiError(Instant.now(), 503, "Service Unavailable",
						List.of("Fotoğraf deposuna şu an erişilemiyor. Lütfen sonra tekrar deneyin.")));
	}

	/**
	 * The container rejected the upload before any of our code ran, so the service's own size
	 * message never had a chance to fire. Without this the member would see a bare 500.
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiError> handleTooLargeUpload(MaxUploadSizeExceededException ex) {
		return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
				.body(new ApiError(Instant.now(), 413, "Payload Too Large",
						List.of("Dosya çok büyük.")));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiError(Instant.now(), 400, "Bad Request", List.of(ex.getMessage())));
	}
}
