package searchengine.service.responses;

import searchengine.DTO.SearchDataDTO;

public class SearchApiResponse implements ApiResponse {
    private boolean result;
    private int count;
    private SearchDataDTO[] data;

    public SearchApiResponse() {
    }

    public SearchApiResponse(boolean result) {
        this.result = result;
    }

    public SearchApiResponse(boolean result, int count, SearchDataDTO[] data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public SearchDataDTO[] getData() {
        return data;
    }

    public void setData(SearchDataDTO[] data) {
        this.data = data;
    }
}
