package searchengine.controllers.responses;

public class TrueApiResponse implements ApiResponse {

    @Override
    public boolean getResult() {
        return true;
    }
}
