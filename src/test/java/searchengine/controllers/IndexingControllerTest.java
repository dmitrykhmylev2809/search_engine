package searchengine.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import searchengine.service.IndexingService;
import searchengine.responses.ApiResponse;

class IndexingControllerTest {

    @Mock
    private IndexingService indexingService;

    private IndexingController indexingController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        indexingController = new IndexingController(indexingService);
    }

    @Test
    void testStartIndexing() {

        ApiResponse expectedApiResponse = new ApiResponse() {
            @Override
            public boolean getResult() {
                return true;
            }
        };
        when(indexingService.startIndexing()).thenReturn(expectedApiResponse);
        ResponseEntity<Object> response = indexingController.startIndexing();
        assertTrue(expectedApiResponse.getResult());
    }

    @Test
    void testStopIndexing() {

        ApiResponse expectedApiResponse = new ApiResponse() {
            @Override
            public boolean getResult() {
                return true;
            }
        };
        when(indexingService.stopIndexing()).thenReturn(expectedApiResponse);
        ResponseEntity<Object> response = indexingController.stopIndexing();
        assertTrue(expectedApiResponse.getResult());
    }

    @Test
    void testPageIndexing() {
        String testUrl = "http://example.com";
        ApiResponse expectedApiResponse = new ApiResponse() {
            @Override
            public boolean getResult() {
                return true;
            }
        };
        when(indexingService.pageIndexing(testUrl)).thenReturn(expectedApiResponse);
        ResponseEntity<Object> response = indexingController.pageIndexing(testUrl);
        assertTrue(expectedApiResponse.getResult());
    }
}

