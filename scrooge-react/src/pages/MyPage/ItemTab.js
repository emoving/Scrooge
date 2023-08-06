import React, { useState } from "react";
import styles from "./ReportTab.module.css";
import CharList from "./CharList";

export default function ItemTab() {
  const [currentTab, setCurrentTab] = useState(1);

  const tabs = [
    {
      id: 1,
      tabTitle: '캐릭터',
      content: <CharList />,
    },
    {
      id: 2,
      tabTitle: '뱃지',
      content:'뱃지 리스트',
    }
  ]

  const handleTabClick = (e) => {
    setCurrentTab(Number(e.target.id))
  }
  return (
    <div className={styles.tabContainer}>
      {/* 탭 버튼 */}
      <div className={styles.tabs}>
        {tabs.map((tab, i) => 
          <button
            key={i}
            id={tab.id}
            disabled={currentTab === tab.id}
            onClick={handleTabClick}
          >
            {tab.tabTitle}
          </button>
        )}
      </div>
      {/* 해당 내용 */}
      <div className={styles.content}>
        {tabs.map((tab, i) =>
          <div key={i}>
            {currentTab === tab.id &&
              <div>
                <p>{tab.content}</p>
              </div>
            }
          </div>
        )}
      </div>
    </div>
  )
}