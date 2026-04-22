/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest.json;

import dev.vida.core.ApiStatus;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Objects;

/**
 * Streaming pull-парсер JSON, совместимый с RFC 8259.
 *
 * <p>Используется для разбора {@code vida.mod.json} и других конфигов. Цели
 * реализации:
 * <ul>
 *   <li>Без сторонних зависимостей (своя кодовая база в {@code :manifest}).</li>
 *   <li>Минимум аллокаций: имена полей и строки выдаются через
 *       {@link #nextName()}/{@link #nextString()}; числа — lazy.</li>
 *   <li>Точные сообщения об ошибках с координатами (line/col).</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * try (JsonReader in = JsonReader.of(source)) {
 *     in.beginObject();
 *     while (in.hasNext()) {
 *         String name = in.nextName();
 *         switch (name) {
 *             case "id" -> id = in.nextString();
 *             case "version" -> version = in.nextString();
 *             default -> in.skipValue();
 *         }
 *     }
 *     in.endObject();
 * }
 * }</pre>
 */
@ApiStatus.Stable
public final class JsonReader implements AutoCloseable {

    /** Максимальная глубина вложенности JSON. Защита от stack overflow / DoS. */
    private static final int MAX_DEPTH = 256;

    private final Reader reader;
    private final char[] buf = new char[4096];
    private int bufPos;
    private int bufLimit;

    /** 1-based координаты текущего символа для диагностики. */
    private int line = 1;
    private int column = 0;

    /** Последний «прочитанный» токен или {@code null}, если его надо вычислить. */
    private JsonToken peeked;

    /**
     * Стек контекстов: {@code OBJ_EMPTY}/{@code OBJ_NONEMPTY}/{@code ARR_EMPTY}/{@code ARR_NONEMPTY}/{@code DOC}.
     * Храним как байты для эффективности.
     */
    private byte[] stack = new byte[32];
    private int stackTop = 0;

    /** Готовое к выдаче значение текущего STRING/NAME/NUMBER/BOOLEAN. */
    private String stringValue;
    private boolean booleanValue;

    // Стек-состояния
    private static final byte S_DOC          = 1;  // верхний уровень: ожидаем ровно одно значение
    private static final byte S_OBJ_EMPTY    = 2;  // только что открыли объект, перед первым name
    private static final byte S_OBJ_NONEMPTY = 3;  // после значения объекта, ожидаем ',' или '}'
    private static final byte S_OBJ_NAME     = 4;  // прочли name, ожидаем ':'
    private static final byte S_OBJ_VALUE    = 5;  // после ':', ожидаем значение
    private static final byte S_ARR_EMPTY    = 6;
    private static final byte S_ARR_NONEMPTY = 7;
    private static final byte S_DONE         = 8;  // документ завершён

    private JsonReader(Reader reader) {
        this.reader = Objects.requireNonNull(reader, "reader");
        push(S_DOC);
    }

    public static JsonReader of(Reader reader) {
        return new JsonReader(reader);
    }

    public static JsonReader of(String json) {
        return new JsonReader(new StringReader(json));
    }

    public int line()   { return line; }
    public int column() { return column; }

    // ----------------------------------------------------------------- API

    /** Возвращает тип следующего токена, не потребляя его. */
    public JsonToken peek() {
        if (peeked == null) {
            peeked = computeNext();
        }
        return peeked;
    }

    /** Есть ли ещё элементы в текущем объекте/массиве. */
    public boolean hasNext() {
        JsonToken t = peek();
        return t != JsonToken.END_OBJECT && t != JsonToken.END_ARRAY;
    }

    public void beginObject() {
        expect(JsonToken.BEGIN_OBJECT);
    }

    public void endObject() {
        expect(JsonToken.END_OBJECT);
    }

    public void beginArray() {
        expect(JsonToken.BEGIN_ARRAY);
    }

    public void endArray() {
        expect(JsonToken.END_ARRAY);
    }

    public String nextName() {
        expect(JsonToken.NAME);
        return stringValue;
    }

    public String nextString() {
        JsonToken t = peek();
        if (t != JsonToken.STRING) {
            throw error("expected STRING, got " + t);
        }
        peeked = null;
        return stringValue;
    }

    /**
     * Читает число как {@code long}. Если значение имеет дробную часть или не
     * помещается в {@code long}, бросается {@link JsonException}.
     */
    public long nextLong() {
        String raw = nextNumberString();
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            throw error("number '" + raw + "' is not a valid long", ex);
        }
    }

    /** Читает число как {@code int}. */
    public int nextInt() {
        long v = nextLong();
        if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
            throw error("number " + v + " overflows int");
        }
        return (int) v;
    }

    /** Читает число как {@code double}. */
    public double nextDouble() {
        String raw = nextNumberString();
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            throw error("number '" + raw + "' is not a valid double", ex);
        }
    }

    /** Читает число без парсинга; полезно, когда нужна точная строка. */
    public String nextNumberString() {
        JsonToken t = peek();
        if (t != JsonToken.NUMBER) {
            throw error("expected NUMBER, got " + t);
        }
        peeked = null;
        return stringValue;
    }

    public boolean nextBoolean() {
        JsonToken t = peek();
        if (t != JsonToken.BOOLEAN) {
            throw error("expected BOOLEAN, got " + t);
        }
        peeked = null;
        return booleanValue;
    }

    public void nextNull() {
        JsonToken t = peek();
        if (t != JsonToken.NULL) {
            throw error("expected NULL, got " + t);
        }
        peeked = null;
    }

    /** Пропускает следующее значение, включая вложенные структуры. */
    public void skipValue() {
        JsonToken t = peek();
        switch (t) {
            case STRING -> nextString();
            case NUMBER -> nextNumberString();
            case BOOLEAN -> nextBoolean();
            case NULL -> nextNull();
            case BEGIN_OBJECT -> {
                beginObject();
                while (hasNext()) {
                    nextName();
                    skipValue();
                }
                endObject();
            }
            case BEGIN_ARRAY -> {
                beginArray();
                while (hasNext()) skipValue();
                endArray();
            }
            case NAME -> throw error("skipValue called after NAME; call skipValue() for value, not key");
            default -> throw error("skipValue: unexpected " + t);
        }
    }

    /**
     * Закрывает подлежащий {@link Reader}. Любая {@link IOException} при
     * закрытии подавляется — закрытие должно быть безопасно из тестов и из
     * пользовательского кода без обязательного try/catch.
     */
    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException ignore) {
            // Намеренно игнорируем: закрытие источника не должно прерывать поток.
        }
    }

    // =====================================================================
    //                             PARSER CORE
    // =====================================================================

    private void expect(JsonToken expected) {
        JsonToken t = peek();
        if (t != expected) {
            throw error("expected " + expected + ", got " + t);
        }
        peeked = null;
    }

    private JsonToken computeNext() {
        byte state = top();
        return switch (state) {
            case S_DOC -> readAtDocRoot();
            case S_OBJ_EMPTY -> readAtObjectStart(true);
            case S_OBJ_NONEMPTY -> readAtObjectStart(false);
            case S_OBJ_NAME -> readAfterName();
            case S_OBJ_VALUE -> readValue();
            case S_ARR_EMPTY -> readAtArrayStart(true);
            case S_ARR_NONEMPTY -> readAtArrayStart(false);
            case S_DONE -> {
                skipWhitespace();
                if (peekChar() >= 0) {
                    throw error("unexpected trailing content after root value");
                }
                yield JsonToken.EOF;
            }
            default -> throw error("internal: bad state " + state);
        };
    }

    private JsonToken readAtDocRoot() {
        skipWhitespace();
        int c = peekChar();
        if (c < 0) {
            replaceTop(S_DONE);
            return JsonToken.EOF;
        }
        return readValue();
    }

    private JsonToken readAtObjectStart(boolean first) {
        skipWhitespace();
        int c = peekChar();
        if (c == '}') {
            consume();
            pop();
            afterValueConsumed();
            return JsonToken.END_OBJECT;
        }
        if (!first) {
            if (c != ',') {
                throw error("expected ',' or '}', got " + charDisplay(c));
            }
            consume();
            skipWhitespace();
            c = peekChar();
        }
        if (c != '"') {
            throw error("expected '\"' for object key, got " + charDisplay(c));
        }
        consume();
        readStringBody();
        replaceTop(S_OBJ_NAME);
        return JsonToken.NAME;
    }

    private JsonToken readAfterName() {
        skipWhitespace();
        int c = readChar();
        if (c != ':') {
            throw error("expected ':' after object key, got " + charDisplay(c));
        }
        replaceTop(S_OBJ_VALUE);
        return readValue();
    }

    private JsonToken readAtArrayStart(boolean first) {
        skipWhitespace();
        int c = peekChar();
        if (c == ']') {
            consume();
            pop();
            afterValueConsumed();
            return JsonToken.END_ARRAY;
        }
        if (!first) {
            if (c != ',') {
                throw error("expected ',' or ']', got " + charDisplay(c));
            }
            consume();
        }
        return readValue();
    }

    /** Общий парсер значения: вызывается как из DOC, так и внутри OBJ/ARR. */
    private JsonToken readValue() {
        skipWhitespace();
        int c = peekChar();
        if (c < 0) {
            throw error("unexpected EOF while expecting value");
        }
        switch (c) {
            case '{' -> {
                consume();
                push(S_OBJ_EMPTY);
                return JsonToken.BEGIN_OBJECT;
            }
            case '[' -> {
                consume();
                push(S_ARR_EMPTY);
                return JsonToken.BEGIN_ARRAY;
            }
            case '"' -> {
                consume();
                readStringBody();
                afterValueConsumed();
                return JsonToken.STRING;
            }
            case 't' -> { readKeyword("true", true); afterValueConsumed(); return JsonToken.BOOLEAN; }
            case 'f' -> { readKeyword("false", false); afterValueConsumed(); return JsonToken.BOOLEAN; }
            case 'n' -> { readKeyword("null"); afterValueConsumed(); return JsonToken.NULL; }
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) {
                    readNumber();
                    afterValueConsumed();
                    return JsonToken.NUMBER;
                }
                throw error("unexpected character " + charDisplay(c));
            }
        }
    }

    /**
     * После того как токен-«значение» выдан, обновляет состояние стека:
     * внутри объекта переходим к «ожидаем `,` или `}`», в массиве — аналогично.
     * Не меняет состояние, если следующий токен — структурный (BEGIN_*), т.к.
     * переход произойдёт при его END.
     */
    private void afterValueConsumed() {
        byte state = top();
        if (state == S_OBJ_VALUE) {
            replaceTop(S_OBJ_NONEMPTY);
        } else if (state == S_ARR_EMPTY) {
            replaceTop(S_ARR_NONEMPTY);
        } else if (state == S_DOC) {
            replaceTop(S_DONE);
        }
        // Для OBJ_NONEMPTY/ARR_NONEMPTY ничего не делаем.
        // Для END_OBJECT/END_ARRAY pop() уже был вызван раньше.
    }

    // --------------------------------------------------- keyword & literals

    private void readKeyword(String keyword, boolean value) {
        readKeyword(keyword);
        booleanValue = value;
    }

    private void readKeyword(String keyword) {
        for (int i = 0; i < keyword.length(); i++) {
            int c = readChar();
            if (c != keyword.charAt(i)) {
                throw error("expected '" + keyword + "', got '" + (char) c + "'");
            }
        }
    }

    // ------------------------------------------------------------ numbers

    private void readNumber() {
        StringBuilder sb = new StringBuilder(16);
        int c = peekChar();
        if (c == '-') { sb.append((char) c); consume(); c = peekChar(); }

        if (c == '0') {
            sb.append('0'); consume();
            c = peekChar();
            if (c >= '0' && c <= '9') {
                throw error("invalid number: leading zero not allowed");
            }
        } else if (c >= '1' && c <= '9') {
            while (c >= '0' && c <= '9') { sb.append((char) c); consume(); c = peekChar(); }
        } else {
            throw error("invalid number: expected digit, got " + charDisplay(c));
        }

        if (c == '.') {
            sb.append('.'); consume(); c = peekChar();
            if (c < '0' || c > '9') throw error("invalid number: expected digit after '.'");
            while (c >= '0' && c <= '9') { sb.append((char) c); consume(); c = peekChar(); }
        }
        if (c == 'e' || c == 'E') {
            sb.append((char) c); consume(); c = peekChar();
            if (c == '+' || c == '-') { sb.append((char) c); consume(); c = peekChar(); }
            if (c < '0' || c > '9') throw error("invalid number: expected digit in exponent");
            while (c >= '0' && c <= '9') { sb.append((char) c); consume(); c = peekChar(); }
        }
        stringValue = sb.toString();
    }

    // ------------------------------------------------------------ strings

    private void readStringBody() {
        StringBuilder sb = new StringBuilder(16);
        while (true) {
            int c = readChar();
            if (c < 0) throw error("unterminated string");
            if (c == '"') break;
            if (c == '\\') {
                int esc = readChar();
                switch (esc) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/'  -> sb.append('/');
                    case 'b'  -> sb.append('\b');
                    case 'f'  -> sb.append('\f');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    case 'u'  -> sb.append(readUnicodeEscape());
                    default   -> throw error("invalid escape '\\" + (char) esc + "'");
                }
            } else if (c < 0x20) {
                throw error("unescaped control character in string: 0x" + Integer.toHexString(c));
            } else {
                sb.append((char) c);
            }
        }
        stringValue = sb.toString();
    }

    private char readUnicodeEscape() {
        int acc = 0;
        for (int i = 0; i < 4; i++) {
            int c = readChar();
            int d;
            if (c >= '0' && c <= '9')      d = c - '0';
            else if (c >= 'a' && c <= 'f') d = 10 + (c - 'a');
            else if (c >= 'A' && c <= 'F') d = 10 + (c - 'A');
            else throw error("invalid \\u escape: expected hex digit, got " + charDisplay(c));
            acc = (acc << 4) | d;
        }
        return (char) acc;
    }

    // ---------------------------------------------- whitespace & I/O core

    private void skipWhitespace() {
        while (true) {
            int c = peekChar();
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                consume();
            } else {
                return;
            }
        }
    }

    private int peekChar() {
        if (bufPos >= bufLimit && !fill()) return -1;
        return buf[bufPos];
    }

    private int readChar() {
        int c = peekChar();
        if (c < 0) return -1;
        consume();
        return c;
    }

    /** Инкрементирует позицию, обновляя line/column. */
    private void consume() {
        char c = buf[bufPos++];
        if (c == '\n') {
            line++;
            column = 0;
        } else {
            column++;
        }
    }

    private boolean fill() {
        if (bufLimit < 0) return false;
        try {
            int n = reader.read(buf, 0, buf.length);
            if (n < 0) {
                bufLimit = -1;
                bufPos = 0;
                return false;
            }
            bufLimit = n;
            bufPos = 0;
            return true;
        } catch (IOException ex) {
            throw error("I/O error while reading JSON", ex);
        }
    }

    // ------------------------------------------------------ stack helpers

    private void push(byte s) {
        if (stackTop >= MAX_DEPTH) throw error("JSON nesting too deep (" + MAX_DEPTH + ")");
        if (stackTop >= stack.length) {
            byte[] grown = new byte[stack.length * 2];
            System.arraycopy(stack, 0, grown, 0, stack.length);
            stack = grown;
        }
        stack[stackTop++] = s;
    }

    private void pop() {
        if (stackTop == 0) throw error("internal: stack underflow");
        stackTop--;
    }

    private byte top() { return stack[stackTop - 1]; }

    private void replaceTop(byte s) { stack[stackTop - 1] = s; }

    // ---------------------------------------------------------- errors

    private JsonException error(String msg) {
        return new JsonException(msg, line, column);
    }

    private JsonException error(String msg, Throwable cause) {
        return new JsonException(msg, line, column, cause);
    }

    private static String charDisplay(int c) {
        if (c < 0) return "<EOF>";
        if (c >= 0x20 && c < 0x7F) return "'" + (char) c + "'";
        return "0x" + Integer.toHexString(c);
    }
}
