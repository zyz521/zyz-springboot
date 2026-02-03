package com.example.xianyu.service;

import com.example.xianyu.entity.Favorite;
import com.example.xianyu.repository.FavoriteRepository;
import org.springframework.stereotype.Service;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    public Favorite addFavorite(Long userId, Long productId) {
        Favorite f = new Favorite();
        f.setUserId(userId);
        f.setProductId(productId);
        return favoriteRepository.save(f);
    }
}



