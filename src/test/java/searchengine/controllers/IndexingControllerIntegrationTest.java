package searchengine.controllers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import searchengine.dao.IndexingDao;
import searchengine.controllers.responses.ApiResponse;
import searchengine.controllers.responses.TrueApiResponse;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IndexingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IndexingDao indexingDao;

    @Test
    void testStartIndexing() throws Exception {
        ApiResponse expectedApiResponse = new TrueApiResponse();
        when(indexingDao.startIndexing()).thenReturn(expectedApiResponse);

        mockMvc.perform(get("/api/startIndexing").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true));

        verify(indexingDao, times(1)).startIndexing();
    }

    @Test
    void testStopIndexing() throws Exception {
        ApiResponse expectedApiResponse = new TrueApiResponse();
        when(indexingDao.stopIndexing()).thenReturn(expectedApiResponse);

        mockMvc.perform(get("/api/stopIndexing").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true));

        verify(indexingDao, times(1)).stopIndexing();
    }

    @Test
    void testPageIndexing() throws Exception {
        String testUrl = "http://example.com";
        ApiResponse expectedApiResponse = new TrueApiResponse();
        when(indexingDao.pageIndexing(testUrl)).thenReturn(expectedApiResponse);

        mockMvc.perform(post("/api/indexPage")
                        .param("url", testUrl)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true));

        verify(indexingDao, times(1)).pageIndexing(testUrl);
    }
}
