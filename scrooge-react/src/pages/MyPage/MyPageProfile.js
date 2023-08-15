import { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import styles from "./MyPageProfile.module.css";
import MyPageExpBar from "./MyPageExpBar";
import Report from "./Report";
import ItemTab from "./ItemTab";

const MyPageProfile = () => {
  const globalToken = useSelector((state) => state.globalToken);

  const [data, setData] = useState([]);
  const [showItemList, setShowItemList] = useState(false);
  const [levelId, setLevelId] = useState(1);
  const [exp, setExp] = useState(0);
  const maxExp = 100;

  const handleEditBtn = () => {
    setShowItemList((prevState) => !prevState);
  };

  const handleModalOpen = () => {
    setModal(true);
  };
  const handleModalClose = () => {
    setShowItemList(true);
    setModal(false);
  };

  useEffect(() => {
    const postData = {
      method: "GET",
      headers: {
        Authorization: globalToken,
      },
    };
    fetch("https://day6scrooge.duckdns.org/api/member/info", postData)
      .then((resp) => resp.json())
      .then((data) => {
        setData(data);
        setExp(data.exp)
        console.log("레벨업 데이터",data.exp)
        setLevelId(data.levelId)
      })
      .catch((error) => console.log(error));
  }, [globalToken]); // [] : globalToken

  const handleGacha = () => {
    //0번 남은 경우 통신 X
    if (gacha === 0) {
      return;
    }

    const putData = {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: globalToken,
      },
    };
    fetch("https://day6scrooge.duckdns.org/api/member/gacha", putData)
      .then((res) => {
        if (!res.ok) {
          throw new Error("캐릭터 가챠 실패");
        }
        return res.json();
      })
      .then((data) => {
        console.log(data);
        setItem(data);
        setGacha(gacha - 1);
        handleModalOpen();
      });
  };

  const handleChange = (newId) => {
    const putData = {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: globalToken,
      },
    };
    fetch(`https://day6scrooge.duckdns.org/api/avatar/${newId}`, putData)
      .then((res) => {
        if (!res.ok) {
          throw new Error("캐릭터 변경 실패");
        }
        return res.json();
      })
      .then((data) => {
        setAvatar(newId);
      });
  };

  return (
    <div className={styles["profile-container"]}>
      {data && data.levelId && data.mainAvatar.id && (
        <div className={styles["profile-content"]}>
          <div className={styles.profileImage}>
            <img
              src={`https://storage.googleapis.com/scroogestorage/avatars/${avatar}-1.png`}
              alt="프로필 사진"
            />
          </div>
          <ul className={styles["profile-info"]}>
            <li>Lv.{data.levelId}</li>
            <li>{data.nickname}</li>

            <li className={styles["items-info"]}>
              <div>
                <img
                  src={`${process.env.PUBLIC_URL}/images/profile-icon.png`}
                  alt="캐릭터 아이콘"
                />
                <span>12</span>
              </div>
              <div>
                <img
                  src={`${process.env.PUBLIC_URL}/images/badge-icon.png`}
                  alt="뱃지 아이콘"
                />
                <span>6</span>
              </div>
            </li>
            <li>
              <div className={styles.btnContainer}>
                <button className={styles.gachaBtn} onClick={handleGacha}>
                  뽑기 {gacha}회 가능
                </button>
              </div>
            </li>
          </ul>
        </div>
      )}
      <MyPageExpBar exp={exp} maxExp={maxExp} />
      {/* <button onClick={increaseExp}>경험치 획득</button>  */}

      <div
        className={`${styles["edit-btn"]} ${showItemList ? "active" : ""}`}
        onClick={handleEditBtn}
      >
        {showItemList ? "소비 리포트 보러 가기" : "캐릭터 뽑으러 가기"}
      </div>
      {showItemList ? (
        <ItemTab handleCharacterChange={handleChange} />
      ) : (
        <Report />
      )}

      {modal && (
        <div>
          <div
            className={styles.modal_overlay}
            onClick={handleModalClose}
          ></div>
          <div className={styles.modalContainer}>
            <img
              src={`https://storage.googleapis.com/scroogestorage/avatars/${item.avatarId}-1.png`}
              className={styles.profile}
              alt={`gacha`}
            />
            <span className={styles.ment}>
              {item.isDuplicated
                ? "이미 가지고 있는 캐릭터에요.."
                : `${item.avatarId}번이 새로 생겼어요!!`}
            </span>
          </div>
        </div>
      )}
    </div>
  );
};

export default MyPageProfile;
