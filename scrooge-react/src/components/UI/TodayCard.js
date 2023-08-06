import React from "react";
import styles from "./TodayCard.module.css";

const TodayCard = (props) => {
  return (
    <div className={styles.card_shadow}>
      <div className={styles.card}> {props.children}</div>
    </div>
  );
};

export default TodayCard;
