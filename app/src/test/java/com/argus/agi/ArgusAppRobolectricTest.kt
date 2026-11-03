package com.argus.agi

import com.argus.agi.model.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArgusAppUnitTest {

  @Test
  fun testTokenizerEncodingDecoding() {
    val tokenizer = Tokenizer()
    val text = "A.R.G.U.S on device neural intelligence"
    val tokens = tokenizer.encode(text, autoRegister = true)
    assertTrue("Tokens should not be empty", tokens.isNotEmpty())
    val decoded = tokenizer.decode(tokens)
    assertEquals(text, decoded)
  }

  @Test
  fun testSpecialTokens() {
    val tokenizer = Tokenizer()
    val text = "${Tokenizer.USER_TOKEN}Hello${Tokenizer.ASSISTANT_TOKEN}"
    val tokens = tokenizer.encode(text, autoRegister = true)
    assertTrue("Tokens should contain user token", tokens.contains(Tokenizer.USER_ID))
    assertTrue("Tokens should contain assistant token", tokens.contains(Tokenizer.ASSISTANT_ID))
  }
}
