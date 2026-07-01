import { Route, Routes } from "react-router-dom";
import { AppSelector } from "@/pages/AppSelector";
import { ActivityHome } from "@/pages/ActivityHome";
import { ActivityTrack } from "@/pages/ActivityTrack";
import { ActivitySummary } from "@/pages/ActivitySummary";
import { ActivityHistory } from "@/pages/ActivityHistory";
import { GymHome } from "@/pages/GymHome";
import { GymLog } from "@/pages/GymLog";
import { GymSummary } from "@/pages/GymSummary";
import { GymHistory } from "@/pages/GymHistory";

export default function App() {
  return (
    <div className="min-h-full flex justify-center bg-background">
      <div className="w-full max-w-[440px] min-h-screen px-5 py-6 flex flex-col">
        <Routes>
          <Route path="/" element={<AppSelector />} />
          <Route path="/:activity" element={<ActivityHome />} />
          <Route path="/:activity/track" element={<ActivityTrack />} />
          <Route path="/:activity/summary/:id" element={<ActivitySummary />} />
          <Route path="/:activity/history" element={<ActivityHistory />} />
          <Route path="/gym" element={<GymHome />} />
          <Route path="/gym/log" element={<GymLog />} />
          <Route path="/gym/summary/:id" element={<GymSummary />} />
          <Route path="/gym/history" element={<GymHistory />} />
        </Routes>
      </div>
    </div>
  );
}
