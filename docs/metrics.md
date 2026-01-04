# Метрики, коротко

Исходный код всех use case лежит в `data-features/feature-api/.../metrics/`.  
Общее правило: если данных нет — ничего не выдумываем, метрика возвращает пустое состояние.

| Метрика | Где почитать | О чем говорит | Когда особенно полезна |
| --- | --- | --- | --- |
| Estimated 1RM | [docs/metrics/estimated-1rm.md](metrics/estimated-1rm.md) | Показывает примерную силу в графике, без сложных формул для пользователя | Работа с базовыми упражнениями, отслеживание прогресса в жиме/тяге/приседе |
| Performance Trend | [docs/metrics/performance-trend.md](metrics/performance-trend.md) | Пять карточек “лучше/хуже/рекорд” для объема, повторов, интенсивности, плотности и длительности | Домашний экран, быстрый апдейт после каждой тренировки |
| Muscle Load Summary | [docs/metrics/muscle-load-summary.md](metrics/muscle-load-summary.md) | Раскладывает нагрузку по мышцам и группам, показывает перекосы | Аналитика тренера, когда нужно понять «кого мы перегружаем» |
| Muscle Load Timeline | [docs/metrics/muscle-load-timeline.md](metrics/muscle-load-timeline.md) | Тепловая карта по времени: какие мышцы работали в разные дни/недели | Планирование мезоциклов, восстановление после травм |
| Training Streak | [docs/metrics/training-streak.md](metrics/training-streak.md) | Подбирает подходящий тип серии (дни/недели/цикл) и мотивирует держать ритм | Домашний экран, виджеты привычек |
