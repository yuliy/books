# Chapter 5. Basic multifield search

## 5.1. Signals and signal modeling
Насколько я понял из описания, `signal` - это по сути фича. Причем под фичой мы можем понимать также, нашелся ли токен в конкретном поле. И тогда встаёт вопрос, как скомбинировать сигнал от нахождения токена в разных полях в финальный таргет.

Также важно понимать, что модель данных (набор и типы полей), используемая в источнике данных, часто не подходит в качестве модели для хранения в поисковом движке.

## 5.2. TMDB-search, the final frontier!
При индексации в Elasticsearch можно передать произвольный json. Но структура индекса при этом всё равно будет плоской.

Например такой документ:
```json
{
  ...
  "cast": [
    {
      "name": "Arnold Schwarzenegger",
      "character": "Terminator"
    },
    {
      "name": "Linda Hamilton",
      "character": "Sarah Connor"
    }
  ],
  ...
}
```

В индексе будет выглядеть следующим образом:
```json
{
  ...
  "cast.name": "Arnold Schwarzenegger Linda Hamilton",
  "cast.character": "Terminator Sarah Connor",
  ...
}
```

Т.е. документ преобразуется в линеаризованное дерево. Но при этом его полностью невозможно восстановить. Например, будет утеряна информация, что `cast` - это список и к которому конкретно элементу списка относится конкретный токен.

## 5.3. Signal modeling in field-centric search
Lucene-based search applications take two general-purpose approaches to ranking multiple fields, as shown in figure 5.4.:
![img](/img/figure_5_4.png)

> **Field-centric** search runs the search string against each field, combining scores after each field is searched in isolation.

> **Term-centric** search works just the opposite, by searching each field on a term-by-term basis. The result is a per-term score that combines each field’s influence for that term.

Я понял разницу между этими двумя подходами следующим образом:
  * **field-centric**: Для каждого поля анализируем запрос, делаем поиск с запросом, содержащим все термы. В итоге получаем скор для данного поля. Получаем таким образом скоры для всех полей. Далее комбинируем эти скоры в итоговый скор.
  * **term-centric**: Сначала анализируем запрос. Далее делаем поиск по каждому терму отдельно. Для каждого токена получаем набор скоров для каждого поля. Комбинируем его в скор для этого токена. Потом комбинируем скоры по всем токенам.

Вот ещё что понял:
  * **field-centric**: Здесь мы сначала считаем скор по всем термам для данного поля. Потом комбинируем скоры по полям.
  * **term-centric**: А здесь сначала считаем скор по всем поля для данного терма. А потом комбинируем скоры по термам.

Elasticsearch bakes field-centric options into the `multi_match` query. It runs the search against each field that’s passed in. For each field, `multi_match` runs query-time analysis on the search string, executing a Boolean search on the resulting tokens, each as a SHOULD clause. In other words, for a given field that uses the English analyzer, the search string goes through the process shown in figure 5.5.:
![img](/img/figure_5_5.png)

`multi_match` может формировать финальный скор по-разному. Основные варианты пожалуй следующие:
  * `best_fields` - Выбирается поле с наибольшим скором. Скор остальных либо отбрасывается, либо плюсуется с некоторым коэффициентом. Пример:
    ```
    score = S_title + tie_breaker × (S_overview + S_cast.name + S_directors.name)
    ```
  * `most_fields` - Трактует скор каждого филда как выражение в булевом поиске. Пример:
    ```
    score = (S_overview + S_title + S_cast.name + S_directors.name ) × coord

    # where coord is:
    Coord = <the number of matching clauses> / <number of total clauses>.
    ```

![img](/img/figure_5_6.png)

Как в Elasticsearch делать поиск по стратегии `best_fields`:
```json
usersSearch = "patrick stewart"
query = {
    "query": {
        "multi_match": {
            "query": usersSearch,
            "fields": ["title", "overview",
                      "cast.name", "directors.name"],
            "type": "best_fields"
        }
    }
}
```

Если так искать, получим полную хрень. Почему?

Во-первых, сравнивать скоры (на основне `TF x IDF`) по разным полям - некорректно. Таким образом можно сравнивать только скоры разных документов по одному полю. И это значит, что применение стратегии `best_fields` вот так в лоб - бессмыслица.

Во-вторых, `TF x IDF`-скоры сильно зависят от пользовательского запроса: скоры сильно смещаются в пользу редких термов (коррелирует с высоким `IDF`). Но пользователь часто ищет, используя популярные термы. Этот момент в книге иллюстрируется поисковым запросом "Patrick Stewart". "stewart" - популярный терм в поле `cast.name` (т.к. популярный актер, много где снялся). Но при этом "stewart" - редкий терм в поле `director.name`, т.к. есть режиссер с такой же фамилией, но он снял мало фильмов. В таком случае документы с режиссером `steward` поднимутся выше. Хотя запрос пользователя точно был про актёра.

Вклад разных полей в финальный скор можно варьировать с помощью бустинга (мультипликативные коэффициенты).

Такой подход починит этот конкретный запрос. Но этот инструмент - про другое. Ведь могут быть запросы, где обратная ситуация. К тому же, данные могут поменяться, тогда придется подбирать другие коэффициенты бустинга.

