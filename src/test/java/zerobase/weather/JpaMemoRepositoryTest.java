package zerobase.weather;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.domain.Memo;
import zerobase.weather.repository.JpaMemoRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@Transactional      // 테스트의 결과가 DB에 적용되지 않도록 트랜잭션 어노테이션 사용(트랜잭션이 롤백됨)
public class JpaMemoRepositoryTest {

    @Autowired
    JpaMemoRepository jpaMemoRepository;

    @Test
    void insertTest(){
        //given

        Memo newMemo = new Memo(10, "this is jpa memo");

        //when
        jpaMemoRepository.save(newMemo);
        //then
        List<Memo> memoList = jpaMemoRepository.findAll();
        assertTrue(memoList.size() > 0);
    }


    @Test
    void findByIdTest(){

        //given
        Memo newMemo = new Memo(11, "jpa");

        //when
        Memo memo = jpaMemoRepository.save(newMemo);
        System.out.println(memo.getId());

        //then
        Optional<Memo> result = jpaMemoRepository.findById(memo.getId());
        assertEquals(result.get().getText(), "jpa");
    }
}
