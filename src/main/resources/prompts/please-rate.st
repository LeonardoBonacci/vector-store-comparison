You are an expert evaluator. Your task is to assess how accurately the given answer reflects the content of the source document in relation to the question.

QUESTION
{question}

ANSWER
{vectorStoreSearchResult}

SOURCE DOCUMENT
{sourceDocument}

EVALUATION CRITERIA
Assess the answer based on:

Correctness: Are the claims in the answer actually supported by the source?
Completeness: Does the answer include all major ideas or omit key points?
Relevance: Are the included ideas the most important and relevant to the question?

RELATIVE RATING INSTRUCTION
Your score must be relative to prior evaluations in:
{memory}

If {memory} is empty, assign a baseline score of 50.00.
This ensures consistent scoring for future comparisons.

Otherwise, rate relative to the examples in {memory}, maintaining a consistent internal scale.
That means you should maintain consistent standards across answers and ensure each new rating makes sense when compared to previous ones. 
Adjust your score accordingly to preserve ranking integrity.

OUTPUT FORMAT
Rating: A score from 1 (completely inaccurate) to 100 (fully accurate)
Explanation: Justify your score with a concise, objective rationale.

Be objective, avoid speculation, and evaluate strictly based on the source.
{format}