package olsh.backend.api_gateway.exception;

public class AssetUploadException extends RuntimeException{

    public static final String code = "500_ASSET_UPLOAD_EXCEPTION";

    public AssetUploadException(String message) {
        super(message);
    }
}
