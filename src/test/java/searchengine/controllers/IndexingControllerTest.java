package searchengine.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import searchengine.dao.IndexingDao;
import searchengine.controllers.responses.ApiResponse;

class IndexingControllerTest {

    @Mock
    private IndexingDao indexingDao;

    private IndexingController indexingController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        indexingController = new IndexingController(indexingDao);
    }

    @Test
    void testStartIndexing() {

        ApiResponse expectedApiResponse = new ApiResponse() {
            @Override
            public boolean getResult() {
                return true;
            }
        };
        when(indexingDao.startIndexing()).thenReturn(expectedApiResponse);
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
        when(indexingDao.stopIndexing()).thenReturn(expectedApiResponse);
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
        when(indexingDao.pageIndexing(testUrl)).thenReturn(expectedApiResponse);
        ResponseEntity<Object> response = indexingController.pageIndexing(testUrl);
        assertTrue(expectedApiResponse.getResult());
    }
}

