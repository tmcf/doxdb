/*
 * Copyright 2011 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.trajano.doxdb.jsonpath.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import net.trajano.doxdb.jsonpath.Filter;
import net.trajano.doxdb.jsonpath.InvalidPathException;
import net.trajano.doxdb.jsonpath.Predicate;
import net.trajano.doxdb.jsonpath.internal.token.ArrayPathToken;
import net.trajano.doxdb.jsonpath.internal.token.PathToken;
import net.trajano.doxdb.jsonpath.internal.token.PredicatePathToken;
import net.trajano.doxdb.jsonpath.internal.token.PropertyPathToken;
import net.trajano.doxdb.jsonpath.internal.token.RootPathToken;
import net.trajano.doxdb.jsonpath.internal.token.ScanPathToken;
import net.trajano.doxdb.jsonpath.internal.token.WildcardPathToken;

import static java.util.Arrays.asList;
import static net.trajano.doxdb.jsonpath.internal.Utils.notEmpty;

public class PathCompiler {

    private static final String PROPERTY_OPEN = "['";
    private static final String PROPERTY_CLOSE = "']";
    private static final char DOCUMENT = '$';
    private static final char ANY = '*';
    private static final char PERIOD = '.';
    private static final char BRACKET_OPEN = '[';
    private static final char BRACKET_CLOSE = ']';
    private static final char SPACE = ' ';
    private static final Cache cache = new Cache(200);


    public static Path compile(final String path, final Predicate... filters) {

        notEmpty(path, "Path may not be null empty");
        try {
            String trimmedPath = path.trim();

            if (trimmedPath.endsWith("..")) {
                throw new InvalidPathException("A path can not end with a scan.");
            }

            LinkedList<Predicate> filterList = new LinkedList<Predicate>(asList(filters));

            if (trimmedPath.charAt(0) != '$' && trimmedPath.charAt(0) != '@') {
                trimmedPath = Utils.concat("$.", trimmedPath);
            }

            boolean isRootPath = (trimmedPath.charAt(0) == '$');

            if (trimmedPath.charAt(0) == '@') {
                trimmedPath = Utils.concat("$", trimmedPath.substring(1));
            }

            if (trimmedPath.length() > 1 &&
                    trimmedPath.charAt(1) != '.' &&
                    trimmedPath.charAt(1) != '[') {
                throw new InvalidPathException("Invalid path " + trimmedPath);
            }

            String cacheKey = Utils.concat(trimmedPath, Boolean.toString(isRootPath), filterList.toString());
            Path p = cache.get(cacheKey);
            if (p != null) {
                return p;
            }

            RootPathToken root = null;


            int i = 0;
            int positions;
            String fragment = "";

            do {
                char current = trimmedPath.charAt(i);

                switch (current) {
                    case SPACE:
                        throw new InvalidPathException("Space not allowed in path");
                    case DOCUMENT:
                        fragment = "$";
                        i++;
                        break;
                    case BRACKET_OPEN:
                        positions = fastForwardUntilClosed(trimmedPath, i);
                        fragment = trimmedPath.substring(i, i + positions);
                        i += positions;
                        break;
                    case PERIOD:
                        i++;
                        if ( i < trimmedPath.length() && trimmedPath.charAt(i) == PERIOD) {
                            //This is a deep scan
                            fragment = "..";
                            i++;
                        } else {
                            positions = fastForward(trimmedPath, i);
                            if (positions == 0) {
                                continue;

                            } else if (positions == 1 && trimmedPath.charAt(i) == '*') {
                                fragment = new String("[*]");
                            } else {
                                assertValidFieldChars(trimmedPath, i, positions);

                                fragment = Utils.concat(PROPERTY_OPEN, trimmedPath.substring(i, i + positions), PROPERTY_CLOSE);
                            }
                            i += positions;
                        }
                        break;
                    case ANY:
                        fragment = new String("[*]");
                        i++;
                        break;
                    default:
                        positions = fastForward(trimmedPath, i);

                        fragment = Utils.concat(PROPERTY_OPEN, trimmedPath.substring(i, i + positions), PROPERTY_CLOSE);
                        i += positions;
                        break;
                }
                if (root == null) {
                    root = (RootPathToken) PathComponentAnalyzer.analyze(fragment, filterList);
                } else {
                    root.append(PathComponentAnalyzer.analyze(fragment, filterList));
                }

            } while (i < trimmedPath.length());

            Path pa = new CompiledPath(root, isRootPath);

            cache.put(cacheKey, pa);

            return pa;

        } catch (Exception ex){
            throw new InvalidPathException(ex);
        }
    }

    private static void assertValidFieldChars(String s, int start, int positions) {
        /*
        int i = start;
        while (i < start + positions) {
            char c = s.charAt(i);

            if (!Character.isLetterOrDigit(c) && c != '-' && c != '_' && c != '$' && c != '@') {
                throw new InvalidPathException("Invalid field name! Use bracket notation if your filed names does not match pattern: ([a-zA-Z@][a-zA-Z0-9@\\$_\\-]*)$");
            }
            i++;
        }
        */
    }

    private static int fastForward(String s, int index) {
        int skipCount = 0;
        while (index < s.length()) {
            char current = s.charAt(index);
            if (current == PERIOD || current == BRACKET_OPEN || current == SPACE) {
                break;
            }
            index++;
            skipCount++;
        }
        return skipCount;
    }

    private static int fastForwardUntilClosed(String s, int index) {
        int skipCount = 0;
        int nestedBrackets = 0;

        //First char is always '[' no need to check it
        index++;
        skipCount++;

        while (index < s.length()) {
            char current = s.charAt(index);

            index++;
            skipCount++;

            if (current == BRACKET_CLOSE && nestedBrackets == 0) {
                break;
            }
            if (current == BRACKET_OPEN) {
                nestedBrackets++;
            }
            if (current == BRACKET_CLOSE) {
                nestedBrackets--;
            }
        }
        return skipCount;
    }


    //---------------------------------------------
    //
    //
    //
    //---------------------------------------------
    static class PathComponentAnalyzer {

        private static final Pattern FILTER_PATTERN = Pattern.compile("^\\[\\s*\\?\\s*[,\\s*\\?]*?\\s*]$"); //[?] or [?, ?, ...]
        private int i;
        private char current;

        private final LinkedList<Predicate> filterList;
        private final String pathFragment;

        PathComponentAnalyzer(String pathFragment, LinkedList<Predicate> filterList) {
            this.pathFragment = pathFragment;
            this.filterList = filterList;
        }

        static PathToken analyze(String pathFragment, LinkedList<Predicate> filterList) {
            return new PathComponentAnalyzer(pathFragment, filterList).analyze();
        }

        public PathToken analyze() {

            if ("$".equals(pathFragment)) return new RootPathToken();
            else if ("..".equals(pathFragment)) return new ScanPathToken();
            else if ("[*]".equals(pathFragment)) return new WildcardPathToken();
            else if (".*".equals(pathFragment)) return new WildcardPathToken();
            else if ("[?]".equals(pathFragment)) return new PredicatePathToken(filterList.poll());

            else if (FILTER_PATTERN.matcher(pathFragment).matches()) {
                final int criteriaCount = Utils.countMatches(pathFragment, "?");
                List<Predicate> filters = new ArrayList<Predicate>(criteriaCount);
                for (int i = 0; i < criteriaCount; i++) {
                    filters.add(filterList.poll());
                }
                return new PredicatePathToken(filters);
            }

            this.i = 0;
            do {
                current = pathFragment.charAt(i);

                switch (current) {
                    case '?':
                        return analyzeCriteriaSequence4();
                    case '\'':
                        return analyzeProperty();
                    default:
                        if (Character.isDigit(current) || current == ':' || current == '-' || current == '@') {
                            return analyzeArraySequence();
                        }
                        i++;
                        break;
                }


            } while (i < pathFragment.length());

            throw new InvalidPathException("Could not analyze path component: " + pathFragment);
        }


        public PathToken analyzeCriteriaSequence4() {
            int[] bounds = findFilterBounds();
            i = bounds[1];

            return new PredicatePathToken(Filter.parse(pathFragment.substring(bounds[0], bounds[1])));
        }

        int[] findFilterBounds(){
            int end = 0;
            int start = i;

            while(pathFragment.charAt(start) != '['){
                start--;
            }

            int mem  = ' ';
            int curr = start;
            boolean inProp          = false;
            int openSquareBracket = 0;
            int openBrackets = 0;
            while(end == 0){
                char c = pathFragment.charAt(curr);
                switch (c){
                    case '(':
                        if(!inProp) openBrackets++;
                        break;
                    case ')':
                        if(!inProp) openBrackets--;
                        break;
                    case '[':
                        if(!inProp) openSquareBracket++;
                        break;
                    case ']':
                        if(!inProp){
                            openSquareBracket--;
                            if(openBrackets == 0){
                                end = curr + 1;
                            }
                        }
                        break;
                    case '\'':
                        if(mem == '\\') {
                            break;
                        }
                        inProp = !inProp;
                        break;
                    default:
                        break;
                }
                mem = c;
                curr++;
            }
            if(openBrackets != 0 || openSquareBracket != 0){
                throw new InvalidPathException("Filter brackets are not balanced");
            }
            return new int[]{start, end};
        }




        //"['foo']"
        private PathToken analyzeProperty() {
            List<String> properties = new ArrayList<String>();
            StringBuilder buffer = new StringBuilder();

            boolean propertyIsOpen = false;

            while (current != ']') {
                switch (current) {
                    case '\'':
                        if (propertyIsOpen) {
                            properties.add(buffer.toString());
                            buffer.setLength(0);
                            propertyIsOpen = false;
                        } else {
                            propertyIsOpen = true;
                        }
                        break;
                    default:
                        if (propertyIsOpen) {
                            buffer.append(current);
                        }
                        break;
                }
                current = pathFragment.charAt(++i);
            }
            return new PropertyPathToken(properties);
        }


        //"[-1:]"  sliceFrom
        //"[:1]"   sliceTo
        //"[0:5]"  sliceBetween
        //"[1]"
        //"[1,2,3]"
        //"[(@.length - 1)]"
        private PathToken analyzeArraySequence() {
            StringBuilder buffer = new StringBuilder();
            List<Integer> numbers = new ArrayList<Integer>();

            boolean contextSize = (current == '@');
            boolean sliceTo = false;
            boolean sliceFrom = false;
            boolean sliceBetween = false;
            boolean indexSequence = false;
            boolean singleIndex = false;

            if (contextSize) {

                current = pathFragment.charAt(++i);
                current = pathFragment.charAt(++i);
                while (current != '-') {
                    if (current == ' ' || current == '(' || current == ')') {
                        current = pathFragment.charAt(++i);
                        continue;
                    }
                    buffer.append(current);
                    current = pathFragment.charAt(++i);
                }
                String function = buffer.toString();
                buffer.setLength(0);
                if (!function.equals("size") && !function.equals("length")) {
                    throw new InvalidPathException("Invalid function: @." + function + ". Supported functions are: [(@.length - n)] and [(@.size() - n)]");
                }
                while (current != ')') {
                    if (current == ' ') {
                        current = pathFragment.charAt(++i);
                        continue;
                    }
                    buffer.append(current);
                    current = pathFragment.charAt(++i);
                }

            } else {


                while (Character.isDigit(current) || current == ',' || current == ' ' || current == ':' || current == '-') {

                    switch (current) {
                        case ' ':
                            break;
                        case ':':
                            if (buffer.length() == 0) {
                                //this is a tail slice [:12]
                                sliceTo = true;
                                current = pathFragment.charAt(++i);
                                while (Character.isDigit(current) || current == ' ' || current == '-') {
                                    if (current != ' ') {
                                        buffer.append(current);
                                    }
                                    current = pathFragment.charAt(++i);
                                }
                                numbers.add(Integer.parseInt(buffer.toString()));
                                buffer.setLength(0);
                            } else {
                                //we now this starts with [12:???
                                numbers.add(Integer.parseInt(buffer.toString()));
                                buffer.setLength(0);
                                current = pathFragment.charAt(++i);

                                //this is a tail slice [:12]
                                while (Character.isDigit(current) || current == ' ' || current == '-') {
                                    if (current != ' ') {
                                        buffer.append(current);
                                    }
                                    current = pathFragment.charAt(++i);
                                }

                                if (buffer.length() == 0) {
                                    sliceFrom = true;
                                } else {
                                    sliceBetween = true;
                                    numbers.add(Integer.parseInt(buffer.toString()));
                                    buffer.setLength(0);
                                }
                            }
                            break;
                        case ',':
                            numbers.add(Integer.parseInt(buffer.toString()));
                            buffer.setLength(0);
                            indexSequence = true;
                            break;
                        default:
                            buffer.append(current);
                            break;
                    }
                    if (current == ']') {
                        break;
                    }
                    current = pathFragment.charAt(++i);
                }
            }
            if (buffer.length() > 0) {
                numbers.add(Integer.parseInt(buffer.toString()));
            }
            singleIndex = (numbers.size() == 1) && !sliceTo && !sliceFrom && !contextSize;

            ArrayPathToken.Operation operation = null;

            if (singleIndex) operation = ArrayPathToken.Operation.SINGLE_INDEX;
            else if (indexSequence) operation = ArrayPathToken.Operation.INDEX_SEQUENCE;
            else if (sliceFrom) operation = ArrayPathToken.Operation.SLICE_FROM;
            else if (sliceTo) operation = ArrayPathToken.Operation.SLICE_TO;
            else if (sliceBetween) operation = ArrayPathToken.Operation.SLICE_BETWEEN;
            else if (contextSize) operation = ArrayPathToken.Operation.CONTEXT_SIZE;

            assert operation != null;

            return new ArrayPathToken(numbers, operation);

        }
    }


}
