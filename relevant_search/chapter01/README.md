# Chapter 1. The search relevance problem
> **Relevance** is the art of ranking content for a search based on how much that content satisfies the needs of the user and the business.
>
> **Relevance** is the practice of improving search results for users by satisfying their information needs in the context of a particular user experience, while balancing how ranking impacts our business’s needs.

**Apache Lucene** is a commonly used Java search library.

**Elasticsearch** and **Apache Solr** are search engines based on Lucene.

Why is search relevance so hard? \
<img src="./img/why_search_relevance_is_hard.png" width="500"/>

Each application has dramatically different relevance expectations. E.g.:
  * web search
  * e-commerce
  * expert search: medicine, law, etc
  * real-estate search
  * restaurant search
  * intranet search
  * search inside some application

This area is very diverse. There's now silver-bullet. Think of Solr or Elactisearch as a search programming framework, not a complete solution.

> In reality, there is a discipline behind relevance: the academic field of **information retrieval**. It has generally accepted practices to improve relevance broadly across many domains. But you’ve seen that what’s relevant depends a great deal on your application.

In *information retrieval*, **relevance** is defined as the practice of returning search results that most satisfy the user's information needs. Further, classic information retrieval focuses on text ranking. Many findings in IR try to measure how likely a given article is going to be relevant to user's text search.

<img src="./img/relevance_judjement_in_quepid.jpeg" width="500"/>

Using  judgment  lists,  researchers  aim  to  measure  whether  changes  to  text  relevance  calculations  improve  the  overall  relevance  of  the  results  across  every  test  collection.

To solve relevance, the relevance engineer:
  1. Identifies salient features describing the content, the user, or the search query
  2. Finds a way to tell the search engine about those features through extraction and enrichment
  3. At search time, measures what’s relevant to a user’s search by crafting signals
  4. Carefully balances the influence of multiple signals to rank results by manipu-lating the ranking function

<img src="./img/duties_of_a_relevance_engineer.png" width="500"/>

A **feature** is an attribute of the content or query. Features drive decisions. Much of the engineering work in search relevance is in feature selection—the act of dis- covering and generating features that give us the appropriate information when a user searches.
