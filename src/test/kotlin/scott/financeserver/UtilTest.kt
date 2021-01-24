package scott.financeserver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SequenceOfListsTest {

    @Test
    fun emptySequence() {
        emptyList<String>().asSequence().sequenceOfLists { true }.toList().run {
            assertThat(this).isEmpty()
        }
    }

    @Test
    fun sequenceOf1() {
        listOf("a" to "1").asSequence().sequenceOfLists { it.second }.toList().let { list ->
            assertThat(list).hasSize(1)
            assertThat(list[0]).hasSize(1)
            assertThat(list[0][0]).isEqualTo("a" to "1")
        }
    }


    @Test
    fun batchesThemTogether() {
        listOf(
            "a" to "1",
            "b" to "1",
            "c" to "2",
            "d" to "2",
            "e" to "3",
            "f" to "3").asSequence().sequenceOfLists { it.second }.toList().let { list ->
                assertThat(list).hasSize(3)
                assertThat(list[0]).hasSize(2)
                assertThat(list[0][0]).isEqualTo("a" to "1")
                assertThat(list[0][1]).isEqualTo("b" to "1")
                assertThat(list[1]).hasSize(2)
                assertThat(list[1][0]).isEqualTo("c" to "2")
                assertThat(list[1][1]).isEqualTo("d" to "2")
                assertThat(list[2]).hasSize(2)
                assertThat(list[2][0]).isEqualTo("e" to "3")
                assertThat(list[2][1]).isEqualTo("f" to "3")
        }
    }


}