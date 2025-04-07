package org.dromara.autotable.annotation.doris.emuns;

/**
 * 索引类型
 *
 * @author lizhian
 */
public enum DorisIndexType {

    /**
     * 倒排索引
     */
    inverted,
    /**
     * NGram BloomFilter 索引
     */
    ngram_bf,
    /**
     * Bitmap 索引
     */
    bitmap;


}
