# Chapter 6. Term-centric search
![img](/img/figure_6_1.png)

## 6.1. What is term-centric search?
![img](/img/figure_6_2.png)

## 6.2. Why do you need term-centric search?
С field-centric search есть следующие проблемы:
  * Albino elephant problem.
  * Signal discordance.

Albino Elephant Problem - это явление, когда документы, у которых с запросом совпало больше термов, ранжируются ниже (речь про текстовое ранжирование а-ля bm25). Это очень просто можно объяснить на следующем примере. Допустим у нас в базе есть следующие 2 документа:
```json
{ "title":"albino", "body": "elephant"}
{ "title":"elephant", "body": "elephant"}
```

Если задать запрос `albino elephant`, то 2ой документ получит бОльший или равный скор.

![img](/img/figure_6_3.png)

Теперь про signal discordance:
![img](/img/figure_6_4.png)

![img](/img/figure_6_5.png)

## 6.3. Performing your first term-centric searches
![img](/img/figure_6_6.png)

![img](/img/figure_6_7.png)

## 6.4. Solving signal discordance in term-centric search
У term-centric подхода в целом тоже есть свои проблемы. Есть два основных способа их решения:
  * Сложить все поля в одно. Ну или хотя бы сложить несколько групп в несколько полей по смыслу.
  * cross-fields - Про это не въехал, что это.

## 6.5. Combining field-centric and term-centric strategies: having your cake and eating it too
В итоге лучший подход - комбинировать в запросе оба подхода. Пример:
![img](/img/listing_6_16.png)
