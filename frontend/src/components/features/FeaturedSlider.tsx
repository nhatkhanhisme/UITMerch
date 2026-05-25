import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';

export interface FeaturedItem {
  id: string;
  name: string;
  orgName?: string;
  desc?: string;
  image: string | null;
  link?: string;
}

interface FeaturedSliderProps {
  items: FeaturedItem[];
  autoPlayInterval?: number;
  isPaused?: boolean;
}

export function FeaturedSlider({ items, autoPlayInterval = 4000, isPaused = false }: FeaturedSliderProps) {
  const [currentIndex, setCurrentIndex] = useState(0);

  const nextSlide = useCallback(() => {
    setCurrentIndex((prev) => (prev === items.length - 1 ? 0 : prev + 1));
  }, [items.length]);

  useEffect(() => {
    if (isPaused) return;
    const timer = setInterval(nextSlide, autoPlayInterval);
    return () => clearInterval(timer);
  }, [nextSlide, autoPlayInterval, isPaused]);

  if (!items?.length) return null;

  return (
    <div className="group relative mb-6 w-full overflow-hidden rounded-[24px] border border-white/30 bg-white/10 shadow-glass backdrop-blur-md sm:mb-10 sm:rounded-[32px]">
      <div
        className="flex h-[22rem] transition-transform duration-700 ease-in-out sm:h-80"
        style={{ transform: `translateX(-${currentIndex * 100}%)` }}
      >
        {items.map((item) => {
          const ContentWrapper = item.link ? Link : 'div';
          return (
            <ContentWrapper
              key={item.id}
              to={item.link || '#'}
              className="flex w-full flex-shrink-0 flex-col gap-4 p-4 transition-opacity hover:opacity-95 sm:flex-row sm:items-center sm:gap-8 sm:p-10"
            >
              <div className="flex h-40 w-full items-center justify-center bg-transparent sm:h-full sm:w-1/2">
              {item.image ? (
                <img
                  src={item.image}
                  alt={item.name}
                  className="max-h-full max-w-full object-contain drop-shadow-xl"
                />
              ) : (
                <span className="font-fredoka text-4xl font-bold text-black-blue/30">{item.name.charAt(0)}</span>
              )}
            </div>

            <div className="flex min-w-0 flex-1 flex-col justify-center sm:w-1/2">
              {item.orgName && (
                <Link to="#" className="mb-2 font-sans text-sm font-medium text-black-blue/60 hover:text-black-blue hover:underline">
                  {item.orgName}
                </Link>
              )}
              <h2 className="mb-2 line-clamp-2 font-fredoka text-xl font-bold tracking-[0.01em] text-black-blue sm:mb-3 sm:text-4xl">{item.name}</h2>
              {item.desc && (
                <p className="mb-4 line-clamp-3 font-sans text-sm font-normal text-black-blue/70 sm:text-base">{item.desc}</p>
              )}
            </div>
            </ContentWrapper>
          );
        })}
      </div>

      <div className="absolute bottom-4 left-0 right-0 flex items-center justify-center gap-2.5 sm:bottom-5">
        {items.map((_, idx) => (
          <button
            key={idx}
            onClick={() => setCurrentIndex(idx)}
            className={`transition-all duration-500 rounded-full ${currentIndex === idx ? 'w-8 h-2 bg-black-blue/60' : 'w-2 h-2 bg-black-blue/20 hover:bg-black-blue/40'
              }`}
          />
        ))}
      </div>
    </div>
  );
}
