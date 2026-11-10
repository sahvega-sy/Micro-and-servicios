package cl.clstore.blogs.repository;

import cl.clstore.blogs.model.Blog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Blog, String> {
}
