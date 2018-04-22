package com.abc.disputes.classification.ml.corenlp;

import com.abc.disputes.classification.data.models.*;
import com.abc.disputes.classification.ml.corenlp.spellchecker.TernarySearchTree;

import io.vavr.Tuple;
import io.vavr.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.abc.common.utils.MLConstants.HYPHEN;
import static com.abc.disputes.classification.ml.corenlp.NLPUtils.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;


public class NLPResource {

    private Logger logger = LoggerFactory.getLogger(NLPResource.class);


    public Corpus preProcessText(List<DisputeDocument> documents, int numberOfGrams) {

        Corpus corpus = Corpus.getInstance();

        documents.stream().map(disputeDocument -> preProcessText(disputeDocument.document,disputeDocument.disputeClass,numberOfGrams)).forEach(row -> corpus.addDocument(row));

        return corpus;
    }


    public DocumentRow preProcessText(String document,int disputeClass,int numberOfGrams) {

        logger.info("Input document is {}",document);

        document = applyContractionExpansions(document.toLowerCase());

        return Stream.of(document).
                map(doc -> Arrays.stream(sentenceDetector.sentDetect(doc)).flatMap(sentence -> Arrays.stream(tokenDetector.tokenize(sentence))).toArray(String[]::new)).
                map(tokens -> Tuple.of(tokens,tagger.tag(tokens), new NamedEntityWrapper(tokens))).
                flatMap(tuple -> mapTokensToNGram(tuple,numberOfGrams).stream()).
                collect(
                        () -> new DocumentRow(disputeClass),
                        (documentRow, term) -> documentRow.addDocumentTerm(term,1),
                        (documentRow1,documentRow2) -> documentRow2.getTermFreqMap().keySet().stream().forEach(term -> documentRow1.addDocumentTerm(term,documentRow2.getTermFreqMap().get(term)))
                );

    }

    private List<String> mapTokensToNGram(Tuple3<String[],String[],NamedEntityWrapper> tokensTagsNamedEntityWrapperTuple,int numberOfGrams) {

        TernarySearchTree searchTree = TernarySearchTree.getInstance();

        logger.info("Named entity wrapper is {} and ngrams is {}",tokensTagsNamedEntityWrapperTuple._3 ,numberOfGrams);

        List<TextAttribute> filteredAttributes =IntStream.range(0,tokensTagsNamedEntityWrapperTuple._1.length).boxed().
                filter(index -> !stopWords.contains(tokensTagsNamedEntityWrapperTuple._1[index]) && !isNumber(tokensTagsNamedEntityWrapperTuple._1[index])).
                map(index -> {

                    String token = tokensTagsNamedEntityWrapperTuple._1[index];
                    String pos = tokensTagsNamedEntityWrapperTuple._2[index];

                    boolean isNamedEntity = tokensTagsNamedEntityWrapperTuple._3.isNamedEntity(index);

                    String lemma = isNamedEntity ? token :
                            lemmatizer.lemmatize(searchTree.containsWord(token) ? token : searchTree.getSuggestions(token).get(0),pos);

                    return new TextAttribute(token, pos, lemma, isNamedEntity, index);
                }).collect(toList());

        return IntStream.range((numberOfGrams-1),filteredAttributes.size()).boxed().
                map(index -> Arrays.stream(formNArrayAttributes(filteredAttributes,index,numberOfGrams)).collect(joining(HYPHEN))).
                collect(toList());

    }

    private String[] formNArrayAttributes(List<TextAttribute> filteredAttributes, int stopIndex,int numberOfGrams) {

        String[] terms = new String[numberOfGrams];

        int index = stopIndex - (numberOfGrams-1);
        int refIndex =0;

        while(index <= stopIndex)
            terms[refIndex++] = filteredAttributes.get(index++).lemma;

        return terms;

    }

    public static void main(String[] args) throws Exception{

        String document ="I did not receive the Fire Wood Rack with my-shipment. I did receive the other 2 items. I have been working with canopy mart to get item delivered, but now they have became unresponsive. I would like refund of $49.99";
        NLPResource resource = new NLPResource();
        System.out.println(resource.preProcessText(document,0,2));

       /* System.out.println(Files.lines(getFilePath("data.txt")).
                filter(line -> !line.isEmpty())
                .map(line -> Integer.parseInt(line.split(" ")[1]))
               // .filter(i -> i > 4266).collect(toList()));
                //.filter(i -> i > 4266)
                .reduce(Integer::max));*/

    }

}
