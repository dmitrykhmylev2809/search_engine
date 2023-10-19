package searchengine.controllers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import searchengine.service.IndexingService;
import searchengine.responses.ApiResponse;
import searchengine.responses.TrueApiResponse;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IndexingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IndexingService indexingService;

    @Test
    void testStartIndexing() throws Exception {
        ApiResponse expectedApiResponse = new TrueApiResponse();
        when(indexingService.startIndexing()).thenReturn(expectedApiResponse);

        mockMvc.perform(get("/api/startIndexing").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true));

        verify(indexingService, times(1)).startIndexing();
    }

    @Test
    void testStopIndexing() throws Exception {
        ApiResponse expectedApiResponse = new TrueApiResponse();
        when(indexingService.stopIndexing()).thenReturn(expectedApiResponse);

        mockMvc.perform(get("/api/stopIndexing").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true));

        verify(indexingService, times(1)).stopIndexing();
    }

    @Test
    void testPageIndexing() throws Exception {
        String testUrl = "http://example.com";
        ApiResponse expectedApiResponse = new TrueApiResponse();
        when(indexingService.pageIndexing(testUrl)).thenReturn(expectedApiResponse);

        mockMvc.perform(post("/api/indexPage")
                        .param("url", testUrl)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true));

        verify(indexingService, times(1)).pageIndexing(testUrl);
    }
}
