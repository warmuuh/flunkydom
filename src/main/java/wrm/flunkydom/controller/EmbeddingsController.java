package wrm.flunkydom.controller;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import wrm.flunkydom.persistence.Embedding;
import wrm.flunkydom.persistence.EmbeddingRepository;
import wrm.flunkydom.service.EmbeddingService;

@Controller
@RequestMapping("/embeddings")
public class EmbeddingsController {


  private final EmbeddingRepository embeddingRepository;
  private final EmbeddingService embeddingService;

  public EmbeddingsController(EmbeddingRepository embeddingRepository, EmbeddingService embeddingService) {
    this.embeddingRepository = embeddingRepository;
    this.embeddingService = embeddingService;
  }

  @GetMapping
  public ModelAndView getEmbeddings() {
    List<Embedding> embeddings = embeddingRepository.findAll();
    return new ModelAndView("embeddings", "model", new GetEmbeddingsModel(embeddings));
  }

  @GetMapping("/details")
  public ModelAndView getEmbeddingDetails(@RequestParam("id") String embeddingId) {
    return new ModelAndView("embedding-detail", "model", new GetEmbeddingDetailModel(embeddingRepository.findById(embeddingId)));
  }

  @PostMapping("/new")
  public RedirectView addEmbedding(@RequestParam("title") String title, @RequestParam("content") String content) {
    embeddingService.addEmbedding(title, content);
    return new RedirectView("/embeddings");
  }

  @PostMapping("/update")
  public RedirectView addEmbedding(@RequestParam("id") String id, @RequestParam("title") String title, @RequestParam("content") String content) {
    embeddingService.updateEmbedding(id, title, content);
    return new RedirectView("/embeddings");
  }

  @PostMapping("/delete")
  public RedirectView deleteEmbedding(@RequestParam("id") String embeddingId) {
    embeddingRepository.deleteById(embeddingId);
    return new RedirectView("/embeddings");
  }

  public record GetEmbeddingsModel(List<Embedding> embeddings){}
  public record GetEmbeddingDetailModel(Embedding embedding){}
}
