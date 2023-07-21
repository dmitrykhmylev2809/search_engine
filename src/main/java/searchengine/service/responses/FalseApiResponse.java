package searchengine.service.responses;

public class FalseApiResponse implements ApiResponse {
    private final String error;

    public FalseApiResponse(String error) {
        this.error = error;
    }

    @Override
    public boolean getResult() {
        return false;
    }

    public String getError() {
        return error;
    }
}
