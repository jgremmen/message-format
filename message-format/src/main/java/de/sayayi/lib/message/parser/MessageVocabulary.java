/*
 * Copyright 2021 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.parser;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.Vocabulary;

import java.util.SortedMap;
import java.util.TreeMap;

import static de.sayayi.lib.message.parser.MessageLexer.*;
import static lombok.AccessLevel.PRIVATE;


/**
 * @author Jeroen Gremmen
 */
@NoArgsConstructor(access = PRIVATE)
enum MessageVocabulary implements Vocabulary
{
  INSTANCE;


  private static final SortedMap<Integer,Name> VOCABULARY = new TreeMap<>();


  static
  {
    add(BOOL, "'true' or 'false'", "BOOL");
    add(COLON, "':'", "COLON");
    add(COMMA, "','", "COMMA");
    add(DOUBLE_QUOTE_END, "\"", "DOUBLE_QUOTE_END");
    add(DOUBLE_QUOTE_START, "\"", "DOUBLE_QUOTE_START");
    add(EMPTY, "'empty'", "EMPTY");
    add(EQ, "'='", "EQ");
    add(GT, "'>'", "GT");
    add(GTE, "'>='", "GTE");
    add(LT, "'<'", "LT");
    add(LTE, "'<='", "LTE");
    add(MAP_END, "'}'", "MAP_END");
    add(MAP_START, "'{'", "MAP_START");
    add(NAME, "<name>", "NAME");
    add(NE, "'<>' or '!'", "NE");
    add(NULL, "'null'", "NULL");
    add(NUMBER, "<number>", "NUMBER");
    add(PARAM_END, "'}'", "PARAM_END");
    add(PARAM_START, "'%{'", "PARAM_START");
    add(SINGLE_QUOTE_END, "'", "SINGLE_QUOTE_END");
    add(SINGLE_QUOTE_START, "'", "SINGLE_QUOTE_START");
  }


  @Override
  public int getMaxTokenType() {
    return VOCABULARY.lastKey();
  }


  @Override
  public String getLiteralName(int tokenType) {
    return VOCABULARY.containsKey(tokenType) ? VOCABULARY.get(tokenType).literal : null;
  }


  @Override
  public String getSymbolicName(int tokenType) {
    return VOCABULARY.containsKey(tokenType) ? VOCABULARY.get(tokenType).symbol : null;
  }


  @Override
  public String getDisplayName(int tokenType) {
    return !VOCABULARY.containsKey(tokenType) ? Integer.toString(tokenType) : VOCABULARY.get(tokenType).literal;
  }


  private static void add(int tokenType, String literal, String symbolic) {
    VOCABULARY.put(tokenType, new Name(literal, symbolic));
  }




  @AllArgsConstructor(access = PRIVATE)
  private static final class Name
  {
    final String literal;
    final String symbol;
  }
}
